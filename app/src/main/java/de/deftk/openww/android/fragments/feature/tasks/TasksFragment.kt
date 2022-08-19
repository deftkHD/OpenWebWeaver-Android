package de.deftk.openww.android.fragments.feature.tasks

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
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
import de.deftk.openww.api.model.Feature
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

        binding.tasksList.adapter = adapter
        binding.tasksList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        tasksViewModel.filteredTasksResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.tasksEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_tasks_failed, response.exception, requireContext())
            }
            enableUI(true)
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
                if (apiContext.user.getGroups().none { Feature.TASKS.isAvailable(it.effectiveRights) }) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }

                tasksViewModel.loadTasks(true, apiContext)
                if (tasksViewModel.allTasksResponse.value == null)
                    enableUI(false)
                binding.fabAddTask.isVisible = apiContext.user.getGroups().any { it.effectiveRights.contains(Permission.TASKS_WRITE) } || apiContext.user.getGroups().any { it.effectiveRights.contains(Permission.TASKS_ADMIN) }
                tasksViewModel.setFilter { filter ->
                    filter.account = apiContext.user.login
                }
            } else {
                binding.fabAddTask.isVisible = false
                binding.tasksEmpty.isVisible = false
                adapter.submitList(emptyList())
                enableUI(false)
            }
        }

        tasksViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetBatchDeleteResponse()
            enableUI(true)

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                actionMode?.finish()
            }
        }

        tasksViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                tasksViewModel.resetPostResponse() // mark as handled
            enableUI(true)

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        registerForContextMenu(binding.tasksList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<Pair<ITask, IOperatingScope>, TasksAdapter.TaskViewHolder> {
        return TasksAdapter(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        menuInflater.inflate(R.menu.tasks_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
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
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.tasks_options_item_show_ignored -> {
                menuItem.isChecked = !menuItem.isChecked
                tasksViewModel.setFilter { filter ->
                    filter.showIgnoredCriteria.value = menuItem.isChecked
                }
            }
            else -> return false
        }
        return true
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.tasks_options_item_show_ignored).isChecked = tasksViewModel.filter.value?.showIgnoredCriteria?.value ?: false
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
            menu.findItem(R.id.tasks_action_item_ignore).isVisible = canIgnore
            val canUnignore = adapter.selectedItems.all { task -> ignored.any { it.id == task.binding.task!!.id && it.scope == task.binding.scope!!.login } }
            menu.findItem(R.id.tasks_action_item_unignore).isVisible = canUnignore

            val canModify = adapter.selectedItems.all { it.binding.scope!!.effectiveRights.contains(Permission.BOARD_WRITE) || it.binding.scope!!.effectiveRights.contains(Permission.BOARD_ADMIN) }
            menu.findItem(R.id.tasks_action_item_delete).isEnabled = canModify
        }

        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tasks_action_item_ignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.ignoreTasks(adapter.selectedItems.map { it.binding.task!! to it.binding.scope!! }, apiContext)
                    enableUI(false)
                }
                mode.finish()
            }
            R.id.tasks_action_item_unignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.unignoreTasks(adapter.selectedItems.map { it.binding.task!! to it.binding.scope!! }, apiContext)
                }
                mode.finish()
            }
            R.id.tasks_action_item_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.batchDelete(adapter.selectedItems.map { it.binding.task!! to it.binding.scope!! }, apiContext)
                    enableUI(false)
                }
                mode.finish()
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val (task, group) = (binding.tasksList.adapter as TasksAdapter).getItem(menuInfo.position)
            requireActivity().menuInflater.inflate(R.menu.tasks_context_menu, menu)
            userViewModel.apiContext.value?.also { apiContext ->
                val ignored = tasksViewModel.getIgnoredTasksBlocking(apiContext).any { it.id == task.id && it.scope == group.login }
                menu.findItem(R.id.tasks_context_item_ignore).isVisible = !ignored
                menu.findItem(R.id.tasks_context_item_unignore).isVisible = ignored
            }
            val canEdit = group.effectiveRights.contains(Permission.TASKS_WRITE) || group.effectiveRights.contains(Permission.TASKS_ADMIN)
            menu.findItem(R.id.tasks_context_item_edit).isVisible = canEdit
            menu.findItem(R.id.tasks_context_item_delete).isVisible = canEdit
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.tasksList.adapter as TasksAdapter
        when (item.itemId) {
            R.id.tasks_context_item_ignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.ignoreTasks(listOf(adapter.getItem(menuInfo.position)), apiContext)
                    enableUI(false)
                }
            }
            R.id.tasks_context_item_unignore -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    tasksViewModel.unignoreTasks(listOf(adapter.getItem(menuInfo.position)), apiContext)
                    enableUI(false)
                }
            }
            R.id.tasks_context_item_import_in_calendar -> {
                val (task, _) = adapter.getItem(menuInfo.position)
                startActivity(CalendarUtil.importTaskIntoCalendar(task))
            }
            R.id.tasks_context_item_edit -> {
                val (task, scope) = adapter.getItem(menuInfo.position)
                val action = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(task.id, scope.login, getString(R.string.edit_task))
                navController.navigate(action)
            }
            R.id.tasks_context_item_delete -> {
                val (task, scope) = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                tasksViewModel.deleteTask(task, scope, apiContext)
                enableUI(false)
            }
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.tasksSwipeRefresh.isEnabled = enabled
        binding.tasksList.isEnabled = enabled
        binding.fabAddTask.isEnabled = enabled
    }
}