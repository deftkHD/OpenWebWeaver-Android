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
                setUIState(if (response.value.isEmpty()) UIState.EMPTY else UIState.READY)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                binding.systemNotificationsEmpty.isVisible = false
                Reporter.reportException(R.string.error_get_system_notifications_failed, response.exception, requireContext())
            }
        }
        binding.systemNotificationList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        userViewModel.systemNotificationDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                userViewModel.resetDeleteResponse() // mark as handled

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        userViewModel.systemNotificationBatchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                userViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                setUIState(UIState.READY)
                actionMode?.finish()
            }
        }

        binding.systemNotificationsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                userViewModel.loadSystemNotifications(apiContext)
                setUIState(UIState.LOADING)
            }
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (userViewModel.allSystemNotificationsResponse.value == null) {
                    userViewModel.loadSystemNotifications(apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.systemNotificationsEmpty.isVisible = false
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        if (userViewModel.systemNotificationFilter.value == null) {
            userViewModel.systemNotificationFilter.value = SystemNotificationFilter(requireContext())
        }
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
            R.id.system_notification_action_item_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    userViewModel.batchDeleteSystemNotifications(adapter.selectedItems.map { it.binding.notification!! }, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        menuInflater.inflate(R.menu.system_notifications_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
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
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.system_notifications_show_settings) {
            val action = SystemNotificationsFragmentDirections.actionSystemNotificationsFragmentToSystemNotificationSettingsFragment()
            navController.navigate(action)
            return true
        } else return false
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
        requireActivity().menuInflater.inflate(R.menu.system_notification_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.systemNotificationList.adapter as SystemNotificationAdapter
        when (item.itemId) {
            R.id.system_notification_context_item_delete -> {
                val notification = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                userViewModel.deleteSystemNotification(notification, apiContext)
                setUIState(UIState.LOADING)
            }
            else -> return false
        }
        return true
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.systemNotificationsEmpty.isVisible = newState.showEmptyIndicator
        binding.systemNotificationList.isEnabled = newState.listEnabled
        binding.systemNotificationsSwipeRefresh.isRefreshing = newState.refreshing
        binding.systemNotificationsSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
    }
}