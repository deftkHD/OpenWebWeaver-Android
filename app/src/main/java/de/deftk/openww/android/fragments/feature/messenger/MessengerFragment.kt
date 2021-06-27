package de.deftk.openww.android.fragments.feature.messenger

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.deftk.openww.android.R
import de.deftk.openww.android.adapter.recycler.ChatAdapter
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.components.ContextMenuRecyclerView
import de.deftk.openww.android.databinding.FragmentMessengerBinding
import de.deftk.openww.android.viewmodel.MessengerViewModel
import de.deftk.openww.android.viewmodel.UserViewModel

class MessengerFragment : Fragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    private val messengerViewModel: MessengerViewModel by activityViewModels()

    private lateinit var binding: FragmentMessengerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMessengerBinding.inflate(inflater, container, false)

        val adapter = ChatAdapter()
        binding.chatList.adapter = adapter
        binding.chatList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.chatsSwipeRefresh.setOnRefreshListener {
            userViewModel.apiContext.value?.also { apiContext ->
                messengerViewModel.loadChats(apiContext)
            }
        }

        messengerViewModel.usersResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Success) {
                adapter.submitList(response.value)
                binding.chatsEmpty.isVisible = response.value.isEmpty()
            } else if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
            binding.progressChats.isVisible = false
            binding.chatsSwipeRefresh.isRefreshing = false
        }

        messengerViewModel.addChatResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
            }
        }

        messengerViewModel.removeChatResponse.observe(viewLifecycleOwner) { response ->
            if (response is Response.Failure) {
                //TODO handle error
                response.exception.printStackTrace()
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
        return binding.root
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

}