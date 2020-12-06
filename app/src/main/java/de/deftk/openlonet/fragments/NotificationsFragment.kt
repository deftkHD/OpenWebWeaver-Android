package de.deftk.openlonet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.BoardNotification
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.activities.feature.board.EditNotificationActivity
import de.deftk.openlonet.activities.feature.board.ReadNotificationActivity
import de.deftk.openlonet.adapter.NotificationAdapter
import de.deftk.openlonet.databinding.FragmentNotificationsBinding
import de.deftk.openlonet.feature.AppFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsFragment: FeatureFragment(AppFeature.FEATURE_NOTIFICATIONS) {

    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        CoroutineScope(Dispatchers.IO).launch {
            refreshNotifications()

            if (AuthStore.getAppUser().groups.any { it.effectiveRights.contains(Permission.BOARD_ADMIN) }) {
                withContext(Dispatchers.Main) {
                    binding.fabAddNotification.visibility = View.VISIBLE
                    binding.fabAddNotification.setOnClickListener {
                        val intent = Intent(context, EditNotificationActivity::class.java)
                        startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_ADD)
                    }
                }
            }
        }

        setHasOptionsMenu(true)

        binding.notificationsSwipeRefresh.setOnRefreshListener {
            binding.notificationList.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                refreshNotifications()
            }
        }
        binding.notificationList.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(context, ReadNotificationActivity::class.java)
            intent.putExtra(ReadNotificationActivity.EXTRA_NOTIFICATION, binding.notificationList.getItemAtPosition(position) as BoardNotification)
            startActivityForResult(intent, 0)
        }

        registerForContextMenu(binding.notificationList)
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
                (binding.notificationList.adapter as Filterable).filter.filter(newText)
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            val notification = binding.notificationList.adapter?.getItem(menuInfo.position) as BoardNotification
            if (notification.operator.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.simple_edit_item_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val notification = binding.notificationList.adapter?.getItem(info.position) as BoardNotification
                val intent = Intent(requireContext(), EditNotificationActivity::class.java)
                intent.putExtra(EditNotificationActivity.EXTRA_NOTIFICATION, notification)
                startActivityForResult(intent, EditNotificationActivity.ACTIVITY_RESULT_EDIT)
                true
            }
            R.id.menu_item_delete -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                val notification = binding.notificationList.adapter?.getItem(info.position) as BoardNotification
                CoroutineScope(Dispatchers.IO).launch {
                    notification.delete()
                    withContext(Dispatchers.Main) {
                        val adapter = binding.notificationList.adapter as NotificationAdapter
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
            val adapter = binding.notificationList.adapter as NotificationAdapter
            val notification = data.getSerializableExtra(EditNotificationActivity.EXTRA_NOTIFICATION) as BoardNotification
            val i = adapter.getPosition(notification)
            adapter.remove(notification)
            adapter.insert(notification, i)
            adapter.notifyDataSetChanged()
        } else if (resultCode == EditNotificationActivity.ACTIVITY_RESULT_ADD && data != null) {
            val adapter = binding.notificationList.adapter as NotificationAdapter
            val notification = data.getSerializableExtra(EditNotificationActivity.EXTRA_NOTIFICATION) as BoardNotification
            adapter.insert(notification, 0)
            adapter.notifyDataSetChanged()
        } else if (resultCode == ReadNotificationActivity.ACTIVITY_RESULT_DELETE && data != null) {
            val adapter = binding.notificationList.adapter as NotificationAdapter
            val notification = data.getSerializableExtra(ReadNotificationActivity.EXTRA_NOTIFICATION) as BoardNotification
            adapter.remove(notification)
            adapter.notifyDataSetChanged()
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun refreshNotifications() {
        try {
            val boardNotifications = AuthStore.getAppUser().getAllBoardNotifications()
            withContext(Dispatchers.Main) {
                binding.notificationList.adapter = NotificationAdapter(requireContext(), boardNotifications)
                binding.notificationsEmpty.isVisible = boardNotifications.isEmpty()
                binding.progressNotifications.visibility = ProgressBar.INVISIBLE
                binding.notificationsSwipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressNotifications.visibility = ProgressBar.INVISIBLE
                binding.notificationsSwipeRefresh.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

}