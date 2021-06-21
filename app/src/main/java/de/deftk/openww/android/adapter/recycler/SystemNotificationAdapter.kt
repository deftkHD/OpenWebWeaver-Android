package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.android.databinding.ListItemSystemNotificationBinding
import de.deftk.openww.android.fragments.feature.systemnotification.SystemNotificationsFragmentDirections

class SystemNotificationAdapter: ListAdapter<ISystemNotification, RecyclerView.ViewHolder>(SystemNotificationDiffCallback()) {

    //TODO highlight unread

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemSystemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SystemNotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notification = getItem(position)
        (holder as SystemNotificationViewHolder).bind(notification)
    }

    class SystemNotificationViewHolder(private val binding: ListItemSystemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(SystemNotificationsFragmentDirections.actionSystemNotificationsFragmentToSystemNotificationFragment(binding.notification!!.id))
            }
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