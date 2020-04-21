package de.deftk.lonet.mobile.fragments

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.Member
import de.deftk.lonet.api.model.feature.Task
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.activities.feature.TaskActivity
import de.deftk.lonet.mobile.adapter.TasksAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_tasks.*

class TasksFragment : FeatureFragment(AppFeature.FEATURE_TASKS) {

    //TODO filters

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View {
        TaskLoader().execute(false)
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.tasks_swipe_refresh)
        val list = view.findViewById<ListView>(R.id.tasks_list)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            TaskLoader().execute(true)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(context, TaskActivity::class.java)
            intent.putExtra(TaskActivity.EXTRA_TASK, list.getItemAtPosition(position) as Task)
            startActivity(intent)
        }
        return view
    }

    private inner class TaskLoader: AsyncTask<Any, Void, List<Task>>() {

        override fun doInBackground(vararg params: Any): List<Task> {
            return AuthStore.appUser.getAllTasks(params[0] == true).sortedByDescending { it.creationDate.time }
        }

        override fun onPostExecute(result: List<Task>) {
            // could be null if fragment is switched while loader continues to run
            progress_tasks?.visibility = ProgressBar.GONE
            tasks_list?.adapter = TasksAdapter(context ?: error("Oops, no context?"), result)
            tasks_swipe_refresh?.isRefreshing = false
        }
    }
}