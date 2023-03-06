package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemNotificationBinding
import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.board.IBoardNotification

class BoardNotificationAdapter(clickListener: ActionModeClickListener<BoardNotificationViewHolder>) : ActionModeAdapter<BoardNotification, BoardNotificationAdapter.BoardNotificationViewHolder>(BoardNotificationDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardNotificationViewHolder {
        val binding = ListItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        @Suppress("UNCHECKED_CAST")
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
            binding.moreButton.visibility = if (group.effectiveRights.contains(Permission.BOARD_WRITE) || group.effectiveRights.contains(Permission.BOARD_ADMIN)) View.VISIBLE else View.INVISIBLE
            binding.executePendingBindings()
        }

    }

}

private class BoardNotificationDiffCallback : DiffUtil.ItemCallback<BoardNotification>() {

    override fun areItemsTheSame(oldItem: BoardNotification, newItem: BoardNotification): Boolean {
        return oldItem.notification.id == newItem.notification.id && oldItem.group.login == newItem.group.login
    }

    override fun areContentsTheSame(oldItem: BoardNotification, newItem: BoardNotification): Boolean {
        return oldItem.notification.equals(newItem.notification) && oldItem.group.login == newItem.group.login
    }
}