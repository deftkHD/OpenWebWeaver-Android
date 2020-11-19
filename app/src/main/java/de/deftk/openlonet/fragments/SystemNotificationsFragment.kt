package de.deftk.openlonet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
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

    //TODO swipe left to delete notification
    // maybe this helps https://www.journaldev.com/23164/android-recyclerview-swipe-to-delete-undo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        CoroutineScope(Dispatchers.IO).launch {
            loadSystemNotifications()
        }

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

    private suspend fun loadSystemNotifications() {
        try {
            val systemNotifications = AuthStore.appUser.getSystemNotifications().sortedByDescending { it.date.time }
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