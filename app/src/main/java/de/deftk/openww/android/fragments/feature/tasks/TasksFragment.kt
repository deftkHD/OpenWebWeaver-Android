package de.deftk.openww.android.fragments.feature.tasks

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.TasksAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentTasksBinding
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.CalendarUtil
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.TasksViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.tasks.ITask

class TasksFragment : ActionModeFragment<Pair<ITask, IOperatingScope>, TasksAdapter.TaskViewHolder>(R.menu.tasks_actionmode_menu), ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentTasksBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        (requireActivity() as? MainActivity?)?.searchProvider = this

        binding.tasksList.adapter = adapter
        binding.tasksList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        tasksViewModel.filteredTasksResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.tasksEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_tasks_failed, response.exception, requireContext())
            }
            binding.progressTasks.visibility = ProgressBar.INVISIBLE
            binding.tasksSwipeRefresh.isRefreshing = false
        }

        binding.tasksSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                tasksViewModel.loadTasks(true, apiContext)
            }
        }

        binding.fabAddTask.setOnClickListener {
            val action = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(null, null, getString(R.string.new_task))
            navController.navigate(action)
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                tasksViewModel.loadTasks(true, apiContext)
                binding.fabAddTask.isVisible = apiContext.user.getGroups().any { it.effectiveRights.contains(Permission.TASKS_WRITE) } || apiContext.user.getGroups().any { it.effectiveRights.contains(Permission.TASKS_ADMIN) }
                tasksViewModel.setFilter { filter ->
                    filter.account = apiContext.user.login
                }
            } else {
                binding.fabAddTask.isVisible = false
                binding.tasksEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressTasks.isVisible = true
            }
        }

        tasksViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                binding.progressTasks.isVisible = false
            } else {
                actionMode?.finish()
            }
        }

        tasksViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetPostResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        setHasOptionsMenu(true)
        registerForContextMenu(binding.tasksList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<Pair<ITask, IOperatingScope>, TasksAdapter.TaskViewHolder> {
        return TasksAdapter(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        inflater.inflate(R.menu.task_list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(tasksViewModel.filter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tasksViewModel.setFilter { filter ->
                    filter.titleCriteria.value = null
                    filter.descriptionCriteria.value = null
                    filter.smartSearchCriteria.value = newText
                }
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_item_show_ignored).isChecked = tasksViewModel.filter.value?.showIgnoredCriteria?.value ?: false
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_show_ignored -> {
                item.isChecked = !item.isChecked
                tasksViewModel.setFilter { filter ->
                    filter.showIgnoredCriteria.value = item.isChecked
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSearchBackPressed(): Boolean {
        return if (searchView.isIconified) {
            false
        } else {
            searchView.isIconified = true
            searchView.setQuery(null, true)
            true
        }
    }

    override fun onItemClick(view: View, viewHolder: TasksAdapter.TaskViewHolder) {
        navController.navigate(TasksFragmentDirections.actionTasksFragmentToReadTaskFragment(viewHolder.binding.task!!.id, viewHolder.binding.scope!!.login))
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        userViewModel.apiContext.value?.also { apiContext ->
            val ignored = tasksViewModel.getIgnoredTasksBlocking(apiContext)
            val canIgnore = adapter.selectedItems.none { task -> ignored.any { it.id == task.binding.task!!.id && it.scope == task.binding.scope!!.login } }
            menu.findItem(R.id.tasks_action_ignore).isVisible = canIgnore
            val canUnignore = adapter.selectedItems.all { task -> ignored.any { it.id == task.binding.task!!.id && it.scope == task.binding.scope!!.login } }
            menu.findItem(R.id.tasks_action_unignore).isVisible = canUnignore

            val canModify = adapter.selectedItems.all { it.binding.scope!!.effectiveRights.contains(Permission.BOARD_WRITE) || it.binding.scope!!.effectiveRights.contains(Permission.BOARD_ADMIN) }
            menu.findItem(R.id.tasks_action_delete).isEnabled = canModify
        }

        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tasks_action_ignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.ignoreTasks(adapter.selectedItems.map { it.binding.task!! to it.binding.scope!! }, apiContext)
                }
                mode.finish()
            }
            R.id.tasks_action_unignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.unignoreTasks(adapter.selectedItems.map { it.binding.task!! to it.binding.scope!! }, apiContext)
                }
                mode.finish()
            }
            R.id.tasks_action_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.batchDelete(adapter.selectedItems.map { it.binding.task!! to it.binding.scope!! }, apiContext)
                    binding.progressTasks.isVisible = true
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val (task, group) = (binding.tasksList.adapter as TasksAdapter).getItem(menuInfo.position)
            requireActivity().menuInflater.inflate(R.menu.task_item_menu, menu)
            userViewModel.apiContext.value?.also { apiContext ->
                val ignored = tasksViewModel.getIgnoredTasksBlocking(apiContext).any { it.id == task.id && it.scope == group.login }
                menu.findItem(R.id.menu_item_ignore).isVisible = !ignored
                menu.findItem(R.id.menu_item_unignore).isVisible = ignored
            }

            if (group.effectiveRights.contains(Permission.TASKS_WRITE) || group.effectiveRights.contains(Permission.TASKS_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.tasksList.adapter as TasksAdapter
        when (item.itemId) {
            R.id.menu_item_ignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.ignoreTasks(listOf(adapter.getItem(menuInfo.position)), apiContext)
                }
            }
            R.id.menu_item_unignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.unignoreTasks(listOf(adapter.getItem(menuInfo.position)), apiContext)
                }
            }
            R.id.menu_item_import_in_calendar -> {
                val (task, _) = adapter.getItem(menuInfo.position)
                startActivity(CalendarUtil.importTaskIntoCalendar(task))
            }
            R.id.menu_item_edit -> {
                val (task, scope) = adapter.getItem(menuInfo.position)
                val action = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(task.id, scope.login, getString(R.string.edit_task))
                navController.navigate(action)
            }
            R.id.menu_item_delete -> {
                val (task, scope) = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                tasksViewModel.deleteTask(task, scope, apiContext)
            }
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    override fun onDestroy() {
        (requireActivity() as? MainActivity?)?.searchProvider = null
        super.onDestroy()
    }

}