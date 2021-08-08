package de.deftk.openww.android.fragments.feature.systemnotification

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.SystemNotificationAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentSystemNotificationsBinding
import de.deftk.openww.android.filter.SystemNotificationFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification

class SystemNotificationsFragment: ActionModeFragment<ISystemNotification, SystemNotificationAdapter.SystemNotificationViewHolder>(R.menu.system_notification_actionmode_menu), ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentSystemNotificationsBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationsBinding.inflate(inflater, container, false)

        binding.systemNotificationList.adapter = adapter
        userViewModel.filteredSystemNotificationResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.systemNotificationsEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                binding.systemNotificationsEmpty.isVisible = false
                Reporter.reportException(R.string.error_get_system_notifications_failed, response.exception, requireContext())
            }
            enableUI(true)
            binding.systemNotificationsSwipeRefresh.isRefreshing = false
        }
        binding.systemNotificationList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        userViewModel.systemNotificationDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                userViewModel.resetDeleteResponse() // mark as handled
            enableUI(true)

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        userViewModel.systemNotificationBatchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                userViewModel.resetBatchDeleteResponse()
            enableUI(true)

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                actionMode?.finish()
            }
        }

        binding.systemNotificationsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                userViewModel.loadSystemNotifications(apiContext)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                userViewModel.loadSystemNotifications(apiContext)
                if (userViewModel.allSystemNotificationsResponse.value == null)
                    enableUI(false)
            } else {
                binding.systemNotificationsEmpty.isVisible = false
                adapter.submitList(emptyList())
                enableUI(false)
            }
        }

        if (userViewModel.systemNotificationFilter.value == null) {
            userViewModel.systemNotificationFilter.value = SystemNotificationFilter(requireContext())
        }
        setHasOptionsMenu(true)
        registerForContextMenu(binding.systemNotificationList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<ISystemNotification, SystemNotificationAdapter.SystemNotificationViewHolder> {
        return SystemNotificationAdapter(this)
    }

    override fun onItemClick(view: View, viewHolder: SystemNotificationAdapter.SystemNotificationViewHolder) {
        navController.navigate(SystemNotificationsFragmentDirections.actionSystemNotificationsFragmentToSystemNotificationFragment(viewHolder.binding.notification!!.id))
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.system_notification_action_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    userViewModel.batchDeleteSystemNotifications(adapter.selectedItems.map { it.binding.notification!! }, apiContext)
                    enableUI(false)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(userViewModel.systemNotificationFilter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = SystemNotificationFilter(requireContext())
                filter.smartSearchCriteria.value = newText
                userViewModel.systemNotificationFilter.value = filter
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onSearchBackPressed(): Boolean {
        return if (searchView.isIconified) {
            false
        } else {
            searchView.isIconified = true
            searchView.setQuery(null, true)
            true
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        requireActivity().menuInflater.inflate(R.menu.delete_menu_item, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.systemNotificationList.adapter as SystemNotificationAdapter
        when (item.itemId) {
            R.id.menu_item_delete -> {
                val notification = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                userViewModel.deleteSystemNotification(notification, apiContext)
                enableUI(true)
            }
            else -> return false
        }
        return true
    }

    override fun onUIStateChanged(enabled: Boolean) {
        binding.systemNotificationsSwipeRefresh.isEnabled = enabled
        binding.systemNotificationList.isEnabled = enabled
    }
}