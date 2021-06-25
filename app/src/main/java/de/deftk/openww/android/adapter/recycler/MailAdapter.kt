package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemMailBinding
import de.deftk.openww.android.fragments.feature.mail.MailFragmentDirections
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder

class MailAdapter : ListAdapter<Pair<IEmail, IEmailFolder>, RecyclerView.ViewHolder>(EmailDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemMailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (email, folder) = getItem(position)
        (holder as EmailViewHolder).bind(email, folder)
    }

    public override fun getItem(position: Int): Pair<IEmail, IEmailFolder> {
        return super.getItem(position)
    }

    class EmailViewHolder(val binding: ListItemMailBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(MailFragmentDirections.actionMailFragmentToReadMailFragment(binding.folder!!.id, binding.email!!.id))
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(email: IEmail, folder: IEmailFolder) {
            binding.email = email
            binding.folder = folder
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