package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemNotificationBinding
import de.deftk.openww.android.fragments.feature.board.NotificationsFragmentDirections
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.IBoardNotification

class BoardNotificationAdapter : ListAdapter<Pair<IBoardNotification, IGroup>, RecyclerView.ViewHolder>(BoardNotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoardNotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (notification, group) = getItem(position)
        (holder as BoardNotificationViewHolder).bind(notification, group)
    }

    public override fun getItem(position: Int): Pair<IBoardNotification, IGroup> {
        return super.getItem(position)
    }

    class BoardNotificationViewHolder(val binding: ListItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(NotificationsFragmentDirections.actionNotificationsFragmentToReadNotificationFragment(binding.notification!!.id, binding.group!!.login))
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
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