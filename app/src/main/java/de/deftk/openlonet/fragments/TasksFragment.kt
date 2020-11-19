package de.deftk.openlonet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.Task
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.activities.feature.tasks.EditTaskActivity
import de.deftk.openlonet.activities.feature.tasks.ReadTaskActivity
import de.deftk.openlonet.adapter.TaskAdapter
import de.deftk.openlonet.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_tasks.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksFragment : FeatureFragment(AppFeature.FEATURE_TASKS) {

    //TODO filters

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        CoroutineScope(Dispatchers.IO).launch {
            refreshTasks()
        }
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.tasks_swipe_refresh)
        val list = view.findViewById<ListView>(R.id.tasks_list)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                refreshTasks()
            }
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(context, ReadTaskActivity::class.java)
            intent.putExtra(ReadTaskActivity.EXTRA_TASK, list.getItemAtPosition(position) as Task)
            startActivityForResult(intent, 0)
        }
        val fabAddTask = view.findViewById<FloatingActionButton>(R.id.fab_add_task)
        if (AuthStore.appUser.groups.any { it.effectiveRights.contains(Permission.TASKS_ADMIN) }) {
            fabAddTask.visibility = View.VISIBLE
            fabAddTask.setOnClickListener {
                val intent = Intent(context, EditTaskActivity::class.java)
                startActivityForResult(intent, EditTaskActivity.ACTIVITY_RESULT_ADD)
            }
        }

        registerForContextMenu(list)
        return view
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            val task = tasks_list.adapter.getItem(menuInfo.position) as Task
            if (task.operator.effectiveRights.contains(Permission.TASKS_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val task = tasks_list.adapter.getItem(info.position) as Task
                val intent = Intent(requireContext(), EditTaskActivity::class.java)
                intent.putExtra(EditTaskActivity.EXTRA_TASK, task)
                startActivityForResult(intent, EditTaskActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val task = tasks_list.adapter.getItem(info.position) as Task
                CoroutineScope(Dispatchers.IO).launch {
                    task.delete()
                    withContext(Dispatchers.Main) {
                        val adapter = tasks_list.adapter as TaskAdapter
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
            val adapter = tasks_list.adapter as TaskAdapter
            val task = data.getSerializableExtra(EditTaskActivity.EXTRA_TASK) as Task
            val i = adapter.getPosition(task)
            adapter.remove(task)
            adapter.insert(task, i)
            adapter.notifyDataSetChanged()
        } else if (resultCode == EditTaskActivity.ACTIVITY_RESULT_ADD && data != null) {
            val adapter = tasks_list.adapter as TaskAdapter
            val task = data.getSerializableExtra(EditTaskActivity.EXTRA_TASK) as Task
            adapter.insert(task, 0)
            adapter.notifyDataSetChanged()
        } else if (resultCode == ReadTaskActivity.ACTIVITY_RESULT_DELETE && data != null) {
            val adapter = tasks_list.adapter as TaskAdapter
            val task = data.getSerializableExtra(EditTaskActivity.EXTRA_TASK) as Task
            adapter.remove(task)
            adapter.notifyDataSetChanged()
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun refreshTasks() {
        try {
            val tasks = AuthStore.appUser.getAllTasks().sortedByDescending { it.creationDate.time }
            withContext(Dispatchers.Main) {
                tasks_list?.adapter = TaskAdapter(requireContext(), tasks)
                tasks_empty?.isVisible = tasks.isEmpty()
                progress_tasks?.visibility = ProgressBar.INVISIBLE
                tasks_swipe_refresh?.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_tasks?.visibility = ProgressBar.INVISIBLE
                tasks_swipe_refresh?.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
            }
        }
    }

}