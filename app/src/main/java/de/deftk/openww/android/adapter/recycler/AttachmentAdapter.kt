package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemAttachmentBinding
import de.deftk.openww.api.model.feature.mailbox.IAttachment

class AttachmentAdapter(val clickListener: AttachmentClickListener) : ListAdapter<IAttachment, AttachmentAdapter.AttachmentViewHolder>(AttachmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        val binding = ListItemAttachmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttachmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        val attachment = getItem(position)
        holder.bind(attachment, clickListener)
    }

    class AttachmentViewHolder(val binding: ListItemAttachmentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(attachment: IAttachment, listener: AttachmentClickListener) {
            binding.attachment = attachment
            binding.setOpenClickListener {
                listener.onOpenAttachment(attachment)
            }
            binding.setSaveClickListener {
                listener.onSaveAttachment(attachment)
            }
            binding.executePendingBindings()
        }

    }

}

class AttachmentDiffCallback : DiffUtil.ItemCallback<IAttachment>() {

    override fun areItemsTheSame(oldItem: IAttachment, newItem: IAttachment): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IAttachment, newItem: IAttachment): Boolean {
        return oldItem.id == newItem.id && oldItem.name == newItem.name && oldItem.size == newItem.size
    }
}

interface AttachmentClickListener {
    fun onSaveAttachment(attachment: IAttachment)
    fun onOpenAttachment(attachment: IAttachment)
}