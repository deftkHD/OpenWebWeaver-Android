package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemNotificationBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.IBoardNotification

class BoardNotificationAdapter(clickListener: ActionModeClickListener<BoardNotificationViewHolder>) : ActionModeAdapter<Pair<IBoardNotification, IGroup>, BoardNotificationAdapter.BoardNotificationViewHolder>(BoardNotificationDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardNotificationViewHolder {
        val binding = ListItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoardNotificationViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: BoardNotificationViewHolder, position: Int) {
        val (notification, group) = getItem(position)
        holder.bind(notification, group)
    }

    class BoardNotificationViewHolder(val binding: ListItemNotificationBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeViewHolder(binding.root, clickListener) {

        private var selected: Boolean = false

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

        fun bind(notification: IBoardNotification, group: IGroup) {
            binding.notification = notification
            binding.group = group
            binding.executePendingBindings()
        }

    }

}

private class BoardNotificationDiffCallback : DiffUtil.ItemCallback<Pair<IBoardNotification, IGroup>>() {

    override fun areItemsTheSame(oldItem: Pair<IBoardNotification, IGroup>, newItem: Pair<IBoardNotification, IGroup>): Boolean {
        return oldItem.first.id == newItem.first.id && oldItem.second.login == newItem.second.login
    }

    override fun areContentsTheSame(oldItem: Pair<IBoardNotification, IGroup>, newItem: Pair<IBoardNotification, IGroup>): Boolean {
        return oldItem.first.equals(newItem.first) && oldItem.second.login == newItem.second.login
    }
}