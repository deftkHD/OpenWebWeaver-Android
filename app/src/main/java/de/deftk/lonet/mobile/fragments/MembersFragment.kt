package de.deftk.lonet.mobile.fragments

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
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.abstract.IBackHandler
import de.deftk.lonet.mobile.adapter.MemberAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_members.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        CoroutineScope(Dispatchers.IO).launch {
            if (group == null) {
                loadMemberGroups()
            } else {
                loadMembers(group)
            }
        }
    }

    private suspend fun loadMemberGroups() {
        try {
            val groups = AuthStore.appUser.getContext().getGroups().sortedBy { it.getName() }
            withContext(Dispatchers.Main) {
                members_list?.adapter = MemberAdapter(requireContext(), groups)
                members_empty?.isVisible = groups.isEmpty()
                members_swipe_refresh?.isRefreshing = false
                progress_members?.visibility = ProgressBar.INVISIBLE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                members_swipe_refresh?.isRefreshing = false
                progress_members?.visibility = ProgressBar.INVISIBLE
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun loadMembers(group: Group) {
        try {
            val members = group.getMembers().sortedBy { it.getName() }
            withContext(Dispatchers.Main) {
                members_list?.adapter = MemberAdapter(requireContext(), members)
                members_empty?.isVisible = members.isEmpty()
                members_swipe_refresh?.isRefreshing = false
                progress_members?.visibility = ProgressBar.INVISIBLE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                members_swipe_refresh?.isRefreshing = false
                progress_members?.visibility = ProgressBar.INVISIBLE
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getTitle(): String {
        return if (currentGroup == null) getString(R.string.members)
        else currentGroup!!.getName()
    }

}