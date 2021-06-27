package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemScopeBinding
import de.deftk.openww.android.fragments.feature.messenger.MessengerFragmentDirections
import de.deftk.openww.api.model.IScope

class ChatAdapter: ListAdapter<IScope, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemScopeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val scope = getItem(position)
        (holder as ChatViewHolder).bind(scope)
    }

    public override fun getItem(position: Int): IScope {
        return super.getItem(position)
    }

    class ChatViewHolder(val binding: ListItemScopeBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener {
                itemView.findNavController().navigate(MessengerFragmentDirections.actionChatsFragmentToMessengerChatFragment(binding.scope!!.login, binding.scope!!.name))
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(scope: IScope) {
            binding.scope = scope
            binding.executePendingBindings()
        }

    }
}

class ChatDiffCallback: DiffUtil.ItemCallback<IScope>() {

    override fun areItemsTheSame(oldItem: IScope, newItem: IScope): Boolean {
        return oldItem.login == newItem.login
    }

    override fun areContentsTheSame(oldItem: IScope, newItem: IScope): Boolean {
        return oldItem.login == newItem.login
    }
}