package de.deftk.openlonet.fragments.feature.systemnotification

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openlonet.R
import de.deftk.openlonet.adapter.recycler.SystemNotificationAdapter
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.databinding.FragmentSystemNotificationsBinding
import de.deftk.openlonet.viewmodel.UserViewModel

class SystemNotificationsFragment: Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var binding: FragmentSystemNotificationsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSystemNotificationsBinding.inflate(inflater, container, false)
        context ?: return binding.root

        val adapter = SystemNotificationAdapter()
        binding.systemNotificationList.adapter = adapter
        userViewModel.systemNotifications.observe(viewLifecycleOwner) { resource ->
            if (resource is Response.Success) {
                adapter.submitList(resource.value)
                binding.systemNotificationsEmpty.isVisible = resource.value.isEmpty()
            } else if (resource is Response.Failure) {
                binding.systemNotificationsEmpty.isVisible = false
                resource.exception.printStackTrace()
                //TODO handle error
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
            if (apiContext != null)
                userViewModel.loadSystemNotifications(apiContext)
        }

        setHasOptionsMenu(true)
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
                //TODO apply filter
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

}