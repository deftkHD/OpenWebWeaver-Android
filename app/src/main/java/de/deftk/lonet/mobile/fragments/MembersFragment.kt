package de.deftk.lonet.mobile.fragments

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.Group
import de.deftk.lonet.api.model.abstract.IManageable
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.adapter.MemberAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_members.*

class MembersFragment: FeatureFragment(AppFeature.FEATURE_MEMBERS), IBackHandler {

    private var currentGroup: Group? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (currentGroup == null)
            navigate(null)
        val view = inflater.inflate(R.layout.fragment_members, container, false)
        val list = view.findViewById<ListView>(R.id.members_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.members_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            navigate(currentGroup)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            if (item is Group) {
                navigate(item)
            }
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            //TODO context menu (send mail, admin features, ...)
            false
        }
        return view
    }

    override fun onBackPressed(): Boolean {
        if (currentGroup != null) {
            navigate(null)
            return true
        }
        return false
    }

    private fun navigate(group: Group?) {
        currentGroup = group
        members_list?.adapter = null
        (activity as AppCompatActivity?)?.supportActionBar?.title = getTitle()
        if (group == null) {
            MemberGroupLoadingTask().execute()
        } else {
            MemberLoadingTask().execute(group)
        }
    }

    override fun getTitle(): String {
        return if (currentGroup == null) getString(R.string.members)
        else currentGroup!!.getName()
    }

    // does not need it's own task in theory
    private inner class MemberGroupLoadingTask: AsyncTask<Group, Void, List<IManageable>?>() {

        override fun doInBackground(vararg params: Group?): List<IManageable>? {
            return try {
                AuthStore.appUser.getContext().getGroups().sortedBy { it.getName() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: List<IManageable>?) {
            members_swipe_refresh?.isRefreshing = false
            progress_members?.visibility = ProgressBar.INVISIBLE
            if (context != null) {
                if (result != null) {
                    members_list?.adapter = MemberAdapter(context!!, result)
                    members_empty?.isVisible = result.isEmpty()
                } else {
                    Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private inner class MemberLoadingTask: AsyncTask<Group, Void, List<IManageable>?>() {

        override fun doInBackground(vararg params: Group?): List<IManageable>? {
            return try {
                params[0]?.getMembers()?.sortedBy { it.getName() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: List<IManageable>?) {
            members_swipe_refresh?.isRefreshing = false
            progress_members?.visibility = ProgressBar.INVISIBLE
            if (context != null) {
                if (result != null) {
                    members_list?.adapter = MemberAdapter(context!!, result)
                    members_empty?.isVisible = result.isEmpty()
                } else {
                    Toast.makeText(context, getString(R.string.request_failed_other).format("No details"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}