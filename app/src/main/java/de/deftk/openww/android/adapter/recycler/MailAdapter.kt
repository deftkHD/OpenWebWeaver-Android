package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemMailBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder

class MailAdapter(clickListener: ActionModeClickListener<MailViewHolder>, var user: IUser) : ActionModeAdapter<Pair<IEmail, IEmailFolder>, MailAdapter.MailViewHolder>(EmailDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MailViewHolder {
        val binding = ListItemMailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MailViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: MailViewHolder, position: Int) {
        val (email, folder) = getItem(position)
        holder.bind(email, folder, user)
    }

    class MailViewHolder(val binding: ListItemMailBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeAdapter.ActionModeViewHolder(binding.root, clickListener) {

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

        fun bind(email: IEmail, folder: IEmailFolder, user: IUser) {
            binding.email = email
            binding.folder = folder
            binding.moreButton.visibility = if (user.effectiveRights.contains(Permission.MAILBOX_WRITE) || user.effectiveRights.contains(Permission.MAILBOX_ADMIN)) View.VISIBLE else View.INVISIBLE
            binding.executePendingBindings()
        }

    }

}

class EmailDiffCallback : DiffUtil.ItemCallback<Pair<IEmail, IEmailFolder>>() {

    override fun areItemsTheSame(oldItem: Pair<IEmail, IEmailFolder>, newItem: Pair<IEmail, IEmailFolder>): Boolean {
        return oldItem.first.id == newItem.first.id && oldItem.second.id == newItem.second.id
    }

    override fun areContentsTheSame(oldItem: Pair<IEmail, IEmailFolder>, newItem: Pair<IEmail, IEmailFolder>): Boolean {
        return oldItem.first.equals(newItem.first) && oldItem.second.id == newItem.second.id
    }
}