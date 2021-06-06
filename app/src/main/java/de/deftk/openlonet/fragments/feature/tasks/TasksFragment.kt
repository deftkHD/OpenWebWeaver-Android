package de.deftk.openlonet.fragments.feature.tasks

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.lonet.api.model.Permission
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.recycler.TasksAdapter
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.components.ContextMenuRecyclerView
import de.deftk.openlonet.databinding.FragmentTasksBinding
import de.deftk.openlonet.viewmodel.TasksViewModel
import de.deftk.openlonet.viewmodel.UserViewModel

class TasksFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private val navController by lazy { findNavController() }
    private lateinit var binding: FragmentTasksBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)

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
            apiContext?.apply {
                if (getUser().getGroups().any { it.effectiveRights.contains(Permission.TASKS_ADMIN) }) {
                    binding.fabAddTask.visibility = View.VISIBLE
                    binding.fabAddTask.setOnClickListener {
                        val action = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(null, null, getString(R.string.new_task))
                        navController.navigate(action)
                    }
                }
                tasksViewModel.loadTasks(this)
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