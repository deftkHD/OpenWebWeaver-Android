package de.deftk.openww.android.fragments.feature.messenger

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
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
import de.deftk.openww.android.filter.ScopeFilter
import de.deftk.openww.android.fragments.ActionModeFragment
import de.deftk.openww.android.utils.Reporter
import de.deftk.openww.android.viewmodel.MessengerViewModel
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IScope

class MessengerFragment : ActionModeFragment<IScope, ChatAdapter.ChatViewHolder>(R.menu.chat_actionmode_menu) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val messengerViewModel: MessengerViewModel by activityViewModels()
    private val navController by lazy { findNavController() }

    private lateinit var binding: FragmentMessengerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMessengerBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

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
            binding.progressChats.isVisible = false
            binding.chatsSwipeRefresh.isRefreshing = false
        }

        messengerViewModel.addChatResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_add_chat_failed, response.exception, requireContext())
            }
        }

        messengerViewModel.removeChatResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Failure) {
                Reporter.reportException(R.string.error_remove_chat_failed, response.exception, requireContext())
            }
        }

        messengerViewModel.batchDeleteResponse.observe(viewLifecycleOwner) { response ->
            if (response != null)
                messengerViewModel.resetBatchDeleteResponse()

            val failure = response?.filterIsInstance<Response.Failure>() ?: return@observe
            if (failure.isNotEmpty()) {
                Reporter.reportException(R.string.error_delete_failed, failure.first().exception, requireContext())
                binding.progressChats.isVisible = false
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
                    messengerViewModel.addChat(input.text.toString(), this)
                }
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        userViewModel.apiContext.observe(viewLifecycleOwner) { apiContext ->
            if (apiContext != null) {
                messengerViewModel.loadChats(apiContext)
            } else {
                binding.chatsEmpty.isVisible = false
                adapter.submitList(emptyList())
                binding.progressChats.isVisible = true
            }
        }

        registerForContextMenu(binding.chatList)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun createAdapter(): ActionModeAdapter<IScope, ChatAdapter.ChatViewHolder> {
        return ChatAdapter(this)
    }

    override fun onItemClick(view: View, viewHolder: ChatAdapter.ChatViewHolder) {
        navController.navigate(MessengerFragmentDirections.actionChatsFragmentToMessengerChatFragment(viewHolder.binding.scope!!.login, viewHolder.binding.scope!!.name))
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat_action_delete -> {
                userViewModel.apiContext.value?.also { apiContext ->
                    messengerViewModel.batchDelete(adapter.selectedItems.map { it.binding.scope!! }, apiContext)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo is ContextMenuRecyclerView.RecyclerViewContextMenuInfo) {
            requireActivity().menuInflater.inflate(R.menu.messenger_chat_menu, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo as ContextMenuRecyclerView.RecyclerViewContextMenuInfo
        val adapter = binding.chatList.adapter as ChatAdapter
        return when (item.itemId) {
            R.id.menu_item_delete -> {
                val user = adapter.getItem(menuInfo.position)
                val apiContext = userViewModel.apiContext.value ?: return false
                messengerViewModel.removeChat(user.login, apiContext)
                true
            }
            else -> false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.list_filter_menu, menu)
        val searchItem = menu.findItem(R.id.filter_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setQuery(messengerViewModel.userFilter.value?.smartSearchCriteria?.value, false) // restore recent search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filter = ScopeFilter()
                filter.smartSearchCriteria.value = newText
                messengerViewModel.userFilter.value = filter
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

}