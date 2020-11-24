package de.deftk.openlonet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.activities.feature.SystemNotificationActivity
import de.deftk.openlonet.adapter.SystemNotificationAdapter
import de.deftk.openlonet.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_system_notifications.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SystemNotificationsFragment: FeatureFragment(AppFeature.FEATURE_SYSTEM_NOTIFICATIONS) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        CoroutineScope(Dispatchers.IO).launch {
            loadSystemNotifications()
        }

        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_system_notifications, container, false)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.system_notifications_swipe_refresh)
        val list = view.findViewById<ListView>(R.id.system_notification_list)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                loadSystemNotifications()
            }
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position) as SystemNotification
            val intent = Intent(context, SystemNotificationActivity::class.java)
            intent.putExtra(SystemNotificationActivity.EXTRA_SYSTEM_NOTIFICATION, item)
            startActivity(intent)
        }
        return view
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
                (system_notification_list.adapter as Filterable).filter.filter(newText)
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    private suspend fun loadSystemNotifications() {
        try {
            val systemNotifications = AuthStore.getAppUser().getSystemNotifications()
            withContext(Dispatchers.Main) {
                system_notification_list?.adapter = SystemNotificationAdapter(requireContext(), systemNotifications)
                system_notifications_empty?.isVisible = systemNotifications.isEmpty()
                progress_system_notifications?.visibility = ProgressBar.INVISIBLE
                system_notifications_swipe_refresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_system_notifications?.visibility = ProgressBar.INVISIBLE
                system_notifications_swipe_refresh.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

}