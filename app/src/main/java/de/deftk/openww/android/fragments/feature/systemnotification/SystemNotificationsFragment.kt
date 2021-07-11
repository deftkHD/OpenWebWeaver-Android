package de.deftk.openww.android.fragments.feature.systemnotification

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
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
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification

class SystemNotificationsFragment: ActionModeFragment<ISystemNotification, SystemNotificationAdapter.SystemNotificationViewHolder>(R.menu.system_notification_actionmode_menu) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentSystemNotificationsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        context ?: return binding.root

        binding.systemNotificationList.adapter = adapter
        userViewModel.systemNotificationsResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.systemNotificationsEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                binding.systemNotificationsEmpty.isVisible = false
                Reporter.reportException(R.string.error_get_system_notifications_failed, response.exception, requireContext())
            }
            binding.progressSystemNotifications.visibility = ProgressBar.INVISIBLE
            binding.systemNotificationsSwipeRefresh.isRefreshing = false
        }
        binding.systemNotificationList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        userViewModel.systemNotificationDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                userViewModel.resetDeleteResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
            }
        }

        userViewModel.systemNotificationBatchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                userViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                binding.progressSystemNotifications.isVisible = false
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
            } else {
                binding.systemNotificationsEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressSystemNotifications.isVisible = true
            }
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
                    binding.progressSystemNotifications.isVisible = true
                }
            }
            else -> return false
        }
        return true
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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
                //TODO apply filter
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }*/

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
            }
            else -> return false
        }
        return true
    }

}