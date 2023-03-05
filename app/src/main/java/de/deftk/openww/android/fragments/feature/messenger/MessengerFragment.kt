package de.deftk.openww.android.fragments.feature.messenger

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.SearchView
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ActionModeAdapter
import de.deftk.openww.android.adapter.recycler.ChatAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentMessengerBinding
import de.deftk.openww.android.feature.messenger.ChatContact
import de.deftk.openww.android.filter.ChatContactFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.ISearchProvider
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.MessengerViewModel
import de.deftk.openww.api.model.Feature

class MessengerFragment : ActionModeFragment<ChatContact, ChatAdapter.ChatViewHolder>(R.menu.messenger_actionmode_menu), ISearchProvider {

    private val messengerViewModel: MessengerViewModel by activityViewModels()
    private val args: MessengerFragmentArgs by navArgs()

    private lateinit var binding: FragmentMessengerBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMessengerBinding.inflate(inflater, container, false)

        if (args.memberLogin != null && args.memberName != null) {
            navController.navigate(MessengerFragmentDirections.actionChatsFragmentToMessengerChatFragment(args.memberLogin!!, args.memberName!!))
        }

        binding.chatList.adapter = adapter
        binding.chatList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.chatsSwipeRefresh.setOnRefreshListener {
            loginViewModel.apiContext.value?.also { apiContext ->
                messengerViewModel.loadChats(apiContext)
                setUIState(UIState.LOADING)
            }
        }

        messengerViewModel.filteredUsersResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                setUIState(if (response.value.isEmpty()) UIState.EMPTY else UIState.READY)
            } else if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_get_users_failed, response.exception, requireContext())
            }
        }

        messengerViewModel.addChatResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                messengerViewModel.resetAddChatResponse()

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_add_chat_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        messengerViewModel.removeChatResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                messengerViewModel.resetRemoveChatResponse()

            if (response is Response.Failure) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_remove_chat_failed, response.exception, requireContext())
            } else if (response is Response.Success) {
                setUIState(UIState.READY)
            }
        }

        messengerViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                messengerViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                setUIState(UIState.ERROR)
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
                setUIState(UIState.READY)
                actionMode?.finish()
            }
        }

        binding.fabAddChat.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.add_chat)

            val input = EditText(requireContext())
            input.hint = getString(R.string.mail)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton(R.string.confirm) { _, _ ->
                loginViewModel.apiContext.value?.apply {
                    var targetUser = input.text.toString()
                    if (!targetUser.contains("@")) {
                        targetUser = "$targetUser@${this.user.login.split("@")[1]}"
                    }
                    messengerViewModel.addChat(targetUser, this)
                    setUIState(UIState.LOADING)
                }
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        loginViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (!Feature.MESSENGER.isAvailable(apiContext.user.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                if (messengerViewModel.allUsersResponse.value == null) {
                    messengerViewModel.loadChats(apiContext)
                    setUIState(UIState.LOADING)
                }
            } else {
                binding.chatsEmpty.isVisible = false
                adapter.submitList(emptyList())
                setUIState(UIState.DISABLED)
            }
        }

        registerForContextMenu(binding.chatList)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<ChatContact, ChatAdapter.ChatViewHolder> {
        return ChatAdapter(this)
    }

    override fun onItemClick(view: View, viewHolder: ChatAdapter.ChatViewHolder) {
        navController.navigate(MessengerFragmentDirections.actionChatsFragmentToMessengerChatFragment(viewHolder.binding.chatContact!!.user.login, viewHolder.binding.chatContact!!.user.name))
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat_action_item_delete -> {
                loginViewModel.apiContext.value?.also { apiContext ->
                    messengerViewModel.batchDelete(adapter.selectedItems.map { it.binding.chatContact!! }, apiContext)
                    setUIState(UIState.LOADING)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            requireActivity().menuInflater.inflate(R.menu.messenger_context_item, menu)

            val adapter = binding.chatList.adapter as ChatAdapter
            val user = adapter.getItem(menuInfo.position)
            menu.findItem(R.id.messenger_context_item_add_chat_contact).isVisible = user.isLocal
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.chatList.adapter as ChatAdapter
        return when (item.itemId) {
            R.id.messenger_context_item_add_chat_contact -> {
                val user = adapter.getItem(menuInfo.position)
                loginViewModel.apiContext.value?.apply {
                    messengerViewModel.addChat(user.user.login, this)
                    setUIState(UIState.LOADING)
                }
                true
            }
            R.id.messenger_context_item_delete -> {
                val user = adapter.getItem(menuInfo.position)
                val apiContext = loginViewModel.apiContext.value ?: return false
                messengerViewModel.removeChat(user, apiContext)
                setUIState(UIState.LOADING)
                true
            }
            else -> false
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.list_options_menu, menu)
        val searchItem = menu.findItem(R.id.list_options_item_search)
        searchView = searchItem.actionView as SearchView
        searchView.setQuery(messengerViewModel.userFilter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = ChatContactFilter()
                filter.smartSearchCriteria.value = newText
                messengerViewModel.userFilter.value = filter
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

    override fun onUIStateChanged(newState: UIState, oldState: UIState) {
        binding.chatsSwipeRefresh.isEnabled = newState.swipeRefreshEnabled
        binding.chatsSwipeRefresh.isRefreshing = newState.refreshing
        binding.fabAddChat.isEnabled = newState == UIState.READY
        binding.chatList.isEnabled = newState.listEnabled
        binding.chatsEmpty.isVisible = newState.showEmptyIndicator
    }
}