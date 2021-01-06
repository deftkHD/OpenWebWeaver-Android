package de.deftk.openlonet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.tasks.Task
import de.deftk.lonet.api.model.Permission
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.activities.feature.tasks.EditTaskActivity
import de.deftk.openlonet.activities.feature.tasks.ReadTaskActivity
import de.deftk.openlonet.adapter.TaskAdapter
import de.deftk.openlonet.databinding.FragmentTasksBinding
import de.deftk.openlonet.feature.AppFeature
import de.deftk.openlonet.utils.getJsonExtra
import de.deftk.openlonet.utils.putJsonExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksFragment : FeatureFragment(AppFeature.FEATURE_TASKS) {

    private lateinit var binding: FragmentTasksBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        binding.tasksSwipeRefresh.setOnRefreshListener {
            binding.tasksList.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                refreshTasks()
            }
        }
        binding.tasksList.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(context, ReadTaskActivity::class.java)
            @Suppress("UNCHECKED_CAST")
            val item = binding.tasksList.getItemAtPosition(position) as Pair<Task, Group>
            intent.putJsonExtra(ReadTaskActivity.EXTRA_TASK, item.first)
            intent.putJsonExtra(ReadTaskActivity.EXTRA_GROUP, item.second)
            startActivityForResult(intent, 0)
        }
        if (AuthStore.getApiUser().getGroups().any { it.effectiveRights.contains(Permission.TASKS_ADMIN) }) {
            binding.fabAddTask.visibility = View.VISIBLE
            binding.fabAddTask.setOnClickListener {
                val intent = Intent(context, EditTaskActivity::class.java)
                startActivityForResult(intent, EditTaskActivity.ACTIVITY_RESULT_ADD)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            refreshTasks()
        }
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
                (binding.tasksList.adapter as Filterable).filter.filter(newText)
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            val task = (binding.tasksList.adapter as TaskAdapter).getItem(menuInfo.position)!!
            if (task.second.effectiveRights.contains(Permission.TASKS_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val task = (binding.tasksList.adapter as TaskAdapter).getItem(info.position)!!
                val intent = Intent(requireContext(), EditTaskActivity::class.java)
                intent.putJsonExtra(EditTaskActivity.EXTRA_TASK, task.first)
                intent.putJsonExtra(EditTaskActivity.EXTRA_GROUP, task.second)
                startActivityForResult(intent, EditTaskActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val task = (binding.tasksList.adapter as TaskAdapter).getItem(info.position)!!
                CoroutineScope(Dispatchers.IO).launch {
                    task.first.delete(task.second.getRequestContext(AuthStore.getApiContext()))
                    withContext(Dispatchers.Main) {
                        val adapter = binding.tasksList.adapter as TaskAdapter
                        adapter.remove(task)
                        adapter.notifyDataSetChanged()
                    }
                }
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == EditTaskActivity.ACTIVITY_RESULT_EDIT && data != null) {
            val adapter = binding.tasksList.adapter as TaskAdapter
            val task = data.getJsonExtra<Task>(EditTaskActivity.EXTRA_TASK)!!
            val group = data.getJsonExtra<Group>(EditTaskActivity.EXTRA_GROUP)!!
            val pair = Pair(task, group)
            val i = adapter.getPosition(pair)
            adapter.remove(pair)
            adapter.insert(pair, i)
            adapter.notifyDataSetChanged()
        } else if (resultCode == EditTaskActivity.ACTIVITY_RESULT_ADD && data != null) {
            val adapter = binding.tasksList.adapter as TaskAdapter
            val task = data.getJsonExtra<Task>(EditTaskActivity.EXTRA_TASK)!!
            val group = data.getJsonExtra<Group>(EditTaskActivity.EXTRA_GROUP)!!
            val pair = Pair(task, group)
            adapter.insert(pair, 0)
            adapter.notifyDataSetChanged()
        } else if (resultCode == ReadTaskActivity.ACTIVITY_RESULT_DELETE && data != null) {
            val adapter = binding.tasksList.adapter as TaskAdapter
            val task = data.getJsonExtra<Task>(EditTaskActivity.EXTRA_TASK)!!
            val group = data.getJsonExtra<Group>(EditTaskActivity.EXTRA_GROUP)!!
            val pair = Pair(task, group)
            adapter.remove(pair)
            adapter.notifyDataSetChanged()
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun refreshTasks() {
        try {
            val tasks = AuthStore.getApiUser().getAllTasks(AuthStore.getApiContext())
            withContext(Dispatchers.Main) {
                binding.tasksList.adapter = TaskAdapter(requireContext(), tasks.map { Pair(it.first as Task, it.second as Group) })
                binding.tasksEmpty.isVisible = tasks.isEmpty()
                binding.progressTasks.visibility = ProgressBar.INVISIBLE
                binding.tasksSwipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressTasks.visibility = ProgressBar.INVISIBLE
                binding.tasksSwipeRefresh.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }
        }
    }

}