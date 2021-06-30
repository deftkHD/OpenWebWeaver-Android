package de.deftk.openww.android.fragments.feature.tasks

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.api.model.Permission
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.TasksAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentTasksBinding
import de.deftk.openww.android.viewmodel.TasksViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class TasksFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var binding: FragmentTasksBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        val adapter = TasksAdapter()
        binding.tasksList.adapter = adapter
        binding.tasksList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        tasksViewModel.tasksResponse.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                adapter.submitList(resource.value)
                binding.tasksEmpty.isVisible = resource.value.isEmpty()
            } else if (resource is Response.Failure) {
                //TODO handle error
                resource.exception.printStackTrace()
            }
            binding.progressTasks.visibility = ProgressBar.INVISIBLE
            binding.tasksSwipeRefresh.isRefreshing = false
        }

        binding.tasksSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                tasksViewModel.loadTasks(apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.getUser().getGroups().any { it.effectiveRights.contains(Permission.TASKS_ADMIN) }) {
                    binding.fabAddTask.visibility = View.VISIBLE
                    binding.fabAddTask.setOnClickListener {
                        val action = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(null, null, getString(R.string.new_task))
                        navController.navigate(action)
                    }
                }
                tasksViewModel.loadTasks(apiContext)
            } else {
                binding.fabAddTask.isVisible = false
                binding.tasksEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressTasks.isVisible = true
            }
        }

        tasksViewModel.postResponse.observe(viewLifecycleOwner) { resource ->
            if (resource != null)
                tasksViewModel.resetPostResponse() // mark as handled

            if (resource is Response.Failure) {
                resource.exception.printStackTrace()
                //TODO handle error
            }
        }

        setHasOptionsMenu(true)
        registerForContextMenu(binding.tasksList)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //TODO search
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val (_, group) = (binding.tasksList.adapter as TasksAdapter).getItem(menuInfo.position)
            if (group.effectiveRights.contains(Permission.TASKS_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.tasksList.adapter as TasksAdapter
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val (task, scope) = adapter.getItem(menuInfo.position)
                val action = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(task.id, scope.login, getString(R.string.edit_task))
                navController.navigate(action)
                true
            }
            R.id.menu_item_delete -> {
                val (task, scope) = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                tasksViewModel.deleteTask(task, scope, apiContext)
                true
            }
            else -> false
        }
    }

}