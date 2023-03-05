package de.deftk.openww.android.fragments.feature.board

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.BoardNotificationAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentNotificationsBinding
import de.deftk.openww.android.filter.BoardNotificationFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.BoardViewModel
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.board.IBoardNotification

class NotificationsFragment: ActionModeFragment<Pair<IBoardNotification, IGroup>, BoardNotificationAdapter.BoardNotificationViewHolder>(R.menu.board_actionmode_menu), ISearchProvider {

    private val boardViewModel: BoardViewModel by activityViewModels()

    private lateinit var binding: FragmentNotificationsBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        binding.notificationList.adapter = adapter
        binding.notificationList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (apiContext.user.getGroups().none { Feature.BOARD.isAvailable(it.effectiveRights) }) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (boardViewModel.allNotificationsResponse.value == null) {
                    boardViewModel.loadBoardNotifications(apiContext)
                    setUIState(UIState.LOADING)
                }
                binding.fabAddNotification.isVisible = apiContext.user.getGroups().any { it.effectiveRights.contains(Permission.BOARD_WRITE) || it.effectiveRights.contains(Permission.BOARD_ADMIN) }
            } else {
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        boardViewModel.filteredNotificationResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                setUIState(if (response.value.isEmpty()) UIState.EMPTY else UIState.READY)
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_notifications_failed, response.exception, requireContext())
                setUIState(UIState.ERROR)
            }
        }

        boardViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                boardViewModel.resetBatchDeleteResponse()
            }

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                setUIState(UIState.ERROR)
            } else {
                actionMode?.finish()
                setUIState(UIState.READY)
            }
        }

        binding.notificationsSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                boardViewModel.loadBoardNotifications(apiContext)
                setUIState(UIState.LOADING)
            }
        }

        binding.fabAddNotification.setOnClickListener {
            val action = NotificationsFragmentDirections.actionNotificationsFragmentToEditNotificationFragment(null, null)
            navController.navigate(action)
        }

        boardViewModel.postResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                boardViewModel.resetPostResponse() // mark as handled

            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_delete_failed, response.exception, requireContext())
                setUIState(UIState.ERROR)
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        registerForContextMenu(binding.notificationList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<Pair<IBoardNotification, IGroup>, BoardNotificationAdapter.BoardNotificationViewHolder> {
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
                loginViewModel.apiContext.value?.also { apiContext ->
                    boardViewModel.batchDelete(adapter.selectedItems.map { it.binding.group!! to it.binding.notification!! }, apiContext)
                    setUIState(UIState.LOADING)
                }
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
        searchView.setQuery(boardViewModel.filter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = BoardNotificationFilter()
                filter.smartSearchCriteria.value = newText
                boardViewModel.filter.value = filter
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
                val (notification, group) = adapter.getItem(menuInfo.position)
                val apiContext = loginViewModel.apiContext.value ?: return false
                boardViewModel.deleteBoardNotification(notification, group, apiContext)
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
    }
}