package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemSystemNotificationBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification

class SystemNotificationAdapter(clickListener: ActionModeClickListener<SystemNotificationViewHolder>): ActionModeAdapter<ISystemNotification, SystemNotificationAdapter.SystemNotificationViewHolder>(SystemNotificationDiffCallback(), clickListener) {

    //TODO highlight unread

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SystemNotificationViewHolder {
        val binding = ListItemSystemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        @Suppress("UNCHECKED_CAST")
        return SystemNotificationViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: SystemNotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    class SystemNotificationViewHolder(val binding: ListItemSystemNotificationBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeViewHolder(binding.root, clickListener) {

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

        fun bind(notification: ISystemNotification) {
            binding.notification = notification
            binding.executePendingBindings()
        }

    }

}

private class SystemNotificationDiffCallback : DiffUtil.ItemCallback<ISystemNotification>() {

    override fun areItemsTheSame(oldItem: ISystemNotification, newItem: ISystemNotification): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ISystemNotification, newItem: ISystemNotification): Boolean {
        return oldItem.equals(newItem)
    }
}