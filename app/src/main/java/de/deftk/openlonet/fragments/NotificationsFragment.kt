package de.deftk.openlonet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.activities.feature.board.EditNotificationActivity
import de.deftk.openlonet.activities.feature.board.ReadNotificationActivity
import de.deftk.openlonet.adapter.NotificationAdapter
import de.deftk.openlonet.feature.AppFeature
import kotlinx.android.synthetic.main.fragment_notifications.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsFragment: FeatureFragment(AppFeature.FEATURE_NOTIFICATIONS) {

    //TODO filters

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        CoroutineScope(Dispatchers.IO).launch {
            refreshNotifications()

            if (AuthStore.appUser.groups.any { it.effectiveRights.contains(Permission.BOARD_ADMIN) }) {
                withContext(Dispatchers.Main) {
                    fab_add_notification?.visibility = View.VISIBLE
                    fab_add_notification?.setOnClickListener {
                        val intent = Intent(context, EditNotificationActivity::class.java)
                        startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_ADD)
                    }
                }
            }
        }

        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        val list = view.findViewById<ListView>(R.id.notification_list)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.notifications_swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            list.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                refreshNotifications()
            }
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(context, ReadNotificationActivity::class.java)
            intent.putExtra(ReadNotificationActivity.EXTRA_NOTIFICATION, list.getItemAtPosition(position) as BoardNotification)
            startActivityForResult(intent, 0)
        }

        registerForContextMenu(list)
        return view
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            val notification = notification_list?.adapter?.getItem(menuInfo.position) as BoardNotification
            if (notification.operator.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val notification = notification_list?.adapter?.getItem(info.position) as BoardNotification
                val intent = Intent(requireContext(), EditNotificationActivity::class.java)
                intent.putExtra(EditNotificationActivity.EXTRA_NOTIFICATION, notification)
                startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val notification = notification_list?.adapter?.getItem(info.position) as BoardNotification
                CoroutineScope(Dispatchers.IO).launch {
                    notification.delete()
                    withContext(Dispatchers.Main) {
                        val adapter = notification_list.adapter as NotificationAdapter
                        adapter.remove(notification)
                        adapter.notifyDataSetChanged()
                    }
                }
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == EditNotificationActivity.ACTIVITY_RESULT_EDIT && data != null) {
            val adapter = notification_list.adapter as NotificationAdapter
            val notification = data.getSerializableExtra(EditNotificationActivity.EXTRA_NOTIFICATION) as BoardNotification
            val i = adapter.getPosition(notification)
            adapter.remove(notification)
            adapter.insert(notification, i)
            adapter.notifyDataSetChanged()
        } else if (resultCode == EditNotificationActivity.ACTIVITY_RESULT_ADD && data != null) {
            val adapter = notification_list.adapter as NotificationAdapter
            val notification = data.getSerializableExtra(EditNotificationActivity.EXTRA_NOTIFICATION) as BoardNotification
            adapter.insert(notification, 0)
            adapter.notifyDataSetChanged()
        } else if (resultCode == ReadNotificationActivity.ACTIVITY_RESULT_DELETE && data != null) {
            val adapter = notification_list.adapter as NotificationAdapter
            val notification = data.getSerializableExtra(ReadNotificationActivity.EXTRA_NOTIFICATION) as BoardNotification
            adapter.remove(notification)
            adapter.notifyDataSetChanged()
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun refreshNotifications() {
        try {
            val boardNotifications = AuthStore.appUser.getAllBoardNotifications().sortedByDescending { it.creationDate.time }
            withContext(Dispatchers.Main) {
                notification_list?.adapter = NotificationAdapter(requireContext(), boardNotifications)
                notifications_empty?.isVisible = boardNotifications.isEmpty()
                progress_notifications?.visibility = ProgressBar.INVISIBLE
                notifications_swipe_refresh?.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress_notifications?.visibility = ProgressBar.INVISIBLE
                notifications_swipe_refresh?.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

}