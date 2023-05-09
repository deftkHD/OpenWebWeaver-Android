package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.BoardNotificationAdapter
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentNotificationsBinding
import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.filter.BoardNotificationFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.fragments.feature.board.viewmodel.NotificationsFragmentUIState
import de.deftk.openww.android.fragments.feature.board.viewmodel.NotificationsViewModel
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.api.model.Permission
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationsFragment: ActionModeFragment<BoardNotification, BoardNotificationAdapter.BoardNotificationViewHolder>(R.menu.board_actionmode_menu), ISearchProvider {

    private val viewModel by viewModels<NotificationsViewModel>()

    private lateinit var binding: FragmentNotificationsBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        binding.notificationList.adapter = adapter
        binding.notificationList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.notificationsSwipeRefresh.setOnRefreshListener {
            viewModel.refreshNotifications()
        }

        binding.fabAddNotification.setOnClickListener {
            navController.navigate(NotificationsFragmentDirections.actionNotificationsFragmentToEditNotificationFragment(null, null))
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is NotificationsFragmentUIState.Loading -> {
                            setUIState(UIState.LOADING)
                        }
                        is NotificationsFragmentUIState.Success -> {
                            binding.fabAddNotification.isVisible = uiState.canAddNotification
                            if (uiState.data.isNotEmpty()) {
                                adapter.submitList(uiState.data)
                                setUIState(UIState.READY)
                            } else {
                                adapter.submitList(emptyList())
                                setUIState(UIState.EMPTY)
                            }
                        }
                        is NotificationsFragmentUIState.Failure -> {
                            setUIState(UIState.ERROR)
                            Reporter.reportException(R.string.error_other, uiState.throwable, requireContext())
                        }
                    }
                }
            }
        }

        registerForContextMenu(binding.notificationList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<BoardNotification, BoardNotificationAdapter.BoardNotificationViewHolder> {
        return BoardNotificationAdapter(this)
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val canModify = adapter.selectedItems.all { it.binding.group!!.effectiveRights.contains(Permission.BOARD_WRITE) || it.binding.group!!.effectiveRights.contains(Permission.BOARD_ADMIN) }
        menu.findItem(R.id.board_action_item_delete).isEnabled = canModify
        return super.onPrepareActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.board_action_item_delete -> {
                viewModel.batchDeleteNotifications(adapter.selectedItems.map { BoardNotification(it.binding.notification!!, it.binding.group!!) })
                setUIState(UIState.LOADING)
            }
            else -> return false
        }
        return true
    }

    override fun onItemClick(view: View, viewHolder: BoardNotificationAdapter.BoardNotificationViewHolder) {
        navController.navigate(NotificationsFragmentDirections.actionNotificationsFragmentToReadNotificationFragment(viewHolder.binding.notification!!.id, viewHolder.binding.group!!.login))
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(viewModel.getFilter().smartSearchCriteria.value, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = BoardNotificationFilter()
                filter.smartSearchCriteria.value = newText
                viewModel.setFilter(filter)
                return true
            }
        })
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
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            val (_, group) = (binding.notificationList.adapter as BoardNotificationAdapter).getItem(menuInfo.position)
            if (group.effectiveRights.contains(Permission.BOARD_WRITE) || group.effectiveRights.contains(Permission.BOARD_ADMIN)) {
                requireActivity().menuInflater.inflate(R.menu.board_context_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.notificationList.adapter as BoardNotificationAdapter
        return when (item.itemId) {
            R.id.board_context_item_edit -> {
                val (notification, group) = adapter.getItem(menuInfo.position)
                val action = NotificationsFragmentDirections.actionNotificationsFragmentToEditNotificationFragment(notification.id, group.login)
                navController.navigate(action)
                true
            }
            R.id.board_context_item_delete -> {
                val notification = adapter.getItem(menuInfo.position)
                viewModel.deleteNotification(notification)
                setUIState(UIState.LOADING)
                true
            }
            else -> false
        }
    }

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.notificationsSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.notificationsSwipeRefresh.isRefreshing = newState.refreshing
        binding.notificationList.isEnabled = newState.listEnabled
        binding.notificationsEmpty.isVisible = newState.showEmptyIndicator
        binding.fabAddNotification.isEnabled = newState == UIState.READY
        if (newState == UIState.READY)
            actionMode?.finish()
    }
}