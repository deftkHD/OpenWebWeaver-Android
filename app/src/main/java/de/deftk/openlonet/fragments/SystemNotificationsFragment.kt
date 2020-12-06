package de.deftk.openlonet.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Filterable
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.isVisible
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.abstract.FeatureFragment
import de.deftk.openlonet.activities.feature.SystemNotificationActivity
import de.deftk.openlonet.adapter.SystemNotificationAdapter
import de.deftk.openlonet.databinding.FragmentSystemNotificationsBinding
import de.deftk.openlonet.feature.AppFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SystemNotificationsFragment: FeatureFragment(AppFeature.FEATURE_SYSTEM_NOTIFICATIONS) {

    private lateinit var binding: FragmentSystemNotificationsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationsBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        binding.systemNotificationsSwipeRefresh.setOnRefreshListener {
            binding.systemNotificationList.adapter = null
            CoroutineScope(Dispatchers.IO).launch {
                loadSystemNotifications()
            }
        }
        binding.systemNotificationList.setOnItemClickListener { _, _, position, _ ->
            val item = binding.systemNotificationList.getItemAtPosition(position) as SystemNotification
            val intent = Intent(context, SystemNotificationActivity::class.java)
            intent.putExtra(SystemNotificationActivity.EXTRA_SYSTEM_NOTIFICATION, item)
            startActivity(intent)
        }
        CoroutineScope(Dispatchers.IO).launch {
            loadSystemNotifications()
        }
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
                (binding.systemNotificationList.adapter as Filterable).filter.filter(newText)
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    private suspend fun loadSystemNotifications() {
        try {
            val systemNotifications = AuthStore.getAppUser().getSystemNotifications()
            withContext(Dispatchers.Main) {
                binding.systemNotificationList.adapter = SystemNotificationAdapter(requireContext(), systemNotifications)
                binding.systemNotificationsEmpty.isVisible = systemNotifications.isEmpty()
                binding.progressSystemNotifications.visibility = ProgressBar.INVISIBLE
                binding.systemNotificationsSwipeRefresh.isRefreshing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressSystemNotifications.visibility = ProgressBar.INVISIBLE
                binding.systemNotificationsSwipeRefresh.isRefreshing = false
                Toast.makeText(context, getString(R.string.request_failed_other).format(e.message ?: e), Toast.LENGTH_LONG).show()
            }
        }
    }

}