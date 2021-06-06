package de.deftk.openlonet.adapter.recycler

import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.databinding.BindingAdapter
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.feature.board.IBoardNotification
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ListItemNotificationBinding
import de.deftk.openlonet.fragments.feature.board.NotificationsFragmentDirections

class BoardNotificationAdapter : ListAdapter<Pair<IBoardNotification, IGroup>, RecyclerView.ViewHolder>(BoardNotificationDiffCallback()) {

    companion object {
        @JvmStatic
        @BindingAdapter("app:backgroundResource")
        fun backgroundResource(view: View, @ColorRes color: Int) {
            view.setBackgroundResource(color)
        }
    }

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