package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemChatContactBinding
import de.deftk.openww.android.feature.messenger.ChatContact
import de.deftk.openww.android.fragments.ActionModeClickListener

class ChatAdapter(clickListener: ActionModeClickListener<ChatViewHolder>): ActionModeAdapter<ChatContact, ChatAdapter.ChatViewHolder>(ChatDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ListItemChatContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        @Suppress("UNCHECKED_CAST")
        return ChatViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatContact = getItem(position)
        holder.bind(chatContact)
    }

    class ChatViewHolder(val binding: ListItemChatContactBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeAdapter.ActionModeViewHolder(binding.root, clickListener) {

        private var selected = false

        init {
            binding.setMenuClickListener {
                itemView.showContextMenu()
            }
        }

        override fun isSelected(): Boolean {
            return selected
        }

        override fun setSelected(selected: Boolean) {
            this.selected = selected
            binding.selected = selected
        }

        fun bind(chatContact: ChatContact) {
            binding.chatContact = chatContact
            binding.executePendingBindings()
        }

    }
}

class ChatDiffCallback: DiffUtil.ItemCallback<ChatContact>() {

    override fun areItemsTheSame(oldItem: ChatContact, newItem: ChatContact): Boolean {
        return oldItem.user.login == newItem.user.login && oldItem.isLocal == newItem.isLocal
    }

    override fun areContentsTheSame(oldItem: ChatContact, newItem: ChatContact): Boolean {
        return oldItem.user.login == newItem.user.login && oldItem.isLocal == newItem.isLocal
    }
}