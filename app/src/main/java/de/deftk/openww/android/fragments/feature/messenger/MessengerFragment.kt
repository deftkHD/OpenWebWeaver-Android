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
import androidx.navigation.fragment.findNavController
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
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.Feature

class MessengerFragment : ActionModeFragment<ChatContact, ChatAdapter.ChatViewHolder>(R.menu.messenger_actionmode_menu), ISearchProvider {

    private val userViewModel: UserViewModel by activityViewModels()
    private val messengerViewModel: MessengerViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentMessengerBinding
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMessengerBinding.inflate(inflater, container, false)

        binding.chatList.adapter = adapter
        binding.chatList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.chatsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                messengerViewModel.loadChats(apiContext)
            }
        }

        messengerViewModel.filteredUsersResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.chatsEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                Reporter.reportException(R.string.error_get_users_failed, response.exception, requireContext())
            }
            enableUI(true)
            binding.chatsSwipeRefresh.isRefreshing = false
        }

        messengerViewModel.addChatResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                messengerViewModel.resetAddChatResponse()

            enableUI(true)
            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_add_chat_failed, response.exception, requireContext())
            }
        }

        messengerViewModel.removeChatResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                messengerViewModel.resetRemoveChatResponse()

            enableUI(true)
            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_remove_chat_failed, response.exception, requireContext())
            }
        }

        messengerViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                messengerViewModel.resetBatchDeleteResponse()
            enableUI(true)

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
            } else {
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
                userViewModel.apiContext.value?.apply {
                    var targetUser = input.text.toString()
                    if (!targetUser.contains("@")) {
                        targetUser = "$targetUser@${this.user.login.split("@")[1]}"
                    }
                    messengerViewModel.addChat(targetUser, this)
                    enableUI(false)
                }
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                if (!Feature.MESSENGER.isAvailable(apiContext.user.effectiveRights)) {
                    Reporter.reportFeatureNotAvailable(requireContext())
                    navController.popBackStack()
                    return@observe
                }
                messengerViewModel.loadChats(apiContext)
                if (messengerViewModel.allUsersResponse.value == null)
                    enableUI(false)
            } else {
                binding.chatsEmpty.isVisible = false
                adapter.submitList(emptyList())
                enableUI(false)
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
                userViewModel.apiContext.value?.also { apiContext ->
                    messengerViewModel.batchDelete(adapter.selectedItems.map { it.binding.chatContact!! }, apiContext)
                    enableUI(false)
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
                userViewModel.apiContext.value?.apply {
                    messengerViewModel.addChat(user.user.login, this)
                    enableUI(false)
                }
                true
            }
            R.id.messenger_context_item_delete -> {
                val user = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                messengerViewModel.removeChat(user, apiContext)
                enableUI(false)
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

    override fun onUIStateChanged(enabled: Boolean) {
        binding.chatsSwipeRefresh.isEnabled = enabled
        binding.chatList.isEnabled = enabled
        binding.fabAddChat.isEnabled = enabled
    }
}