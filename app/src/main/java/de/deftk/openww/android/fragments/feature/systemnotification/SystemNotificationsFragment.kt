package de.deftk.openww.android.fragments.feature.systemnotification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.SystemNotificationAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.databinding.FragmentSystemNotificationsBinding
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.UserViewModel

class SystemNotificationsFragment: Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var binding: FragmentSystemNotificationsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        context ?: return binding.root

        val adapter = SystemNotificationAdapter()
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
        return binding.root
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

}