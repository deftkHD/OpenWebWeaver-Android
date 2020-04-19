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
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.abstract.FeatureFragment
import de.deftk.lonet.mobile.activities.feature.SystemNotificationActivity
import de.deftk.lonet.mobile.adapter.SystemNotificationAdapter
import de.deftk.lonet.mobile.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_system_notifications.*

class SystemNotificationsFragment(): FeatureFragment(AppFeature.FEATURE_SYSTEM_NOTIFICATIONS) {

    //TODO swipe left to delete notification
    // maybe this helps https://www.journaldev.com/23164/android-recyclerview-swipe-to-delete-undo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        SystemNotificationLoader().execute(false)

        val view = inflater.inflate(R.layout.fragment_system_notifications, container, false)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.system_notifications_swipe_refresh)
        val list = view.findViewById<ListView>(R.id.system_notification_list)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            SystemNotificationLoader().execute(true)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position)
            val intent = Intent(context, SystemNotificationActivity::class.java)
            intent.putExtra(SystemNotificationActivity.EXTRA_HASHCODE, item.hashCode())
            startActivity(intent)
        }
        return view
    }

    private inner class SystemNotificationLoader: AsyncTask<Boolean, Void, List<SystemNotification>>() {

        override fun doInBackground(vararg params: Boolean?): List<SystemNotification> {
            return AuthStore.appUser.getSystemNofications(params[0] == true).sortedByDescending { it.date.time }
        }

        override fun onPostExecute(result: List<SystemNotification>) {
            // could be null if fragment is switched while loader continues to run
            progress_system_notifications?.visibility = ProgressBar.GONE
            system_notification_list?.adapter = SystemNotificationAdapter(context ?: error("Oops, no context?"), result)
            system_notifications_swipe_refresh.isRefreshing = false
        }
    }

}