package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemContactDetailBinding
import de.deftk.openww.android.feature.contacts.ContactDetail
import de.deftk.openww.android.feature.contacts.GenderTranslation
import de.deftk.openww.api.model.feature.contacts.Gender

class ContactDetailAdapter(private val editable: Boolean, private val clickListener: ContactDetailClickListener?) : ListAdapter<ContactDetail, RecyclerView.ViewHolder>(ContactDetailDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemContactDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val detail = getItem(position)
        (holder as ContactDetailViewHolder).bind(detail, editable, clickListener)
    }

    class ContactDetailViewHolder(val binding: ListItemContactDetailBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(detail: ContactDetail, editable: Boolean, clickListener: ContactDetailClickListener?) {
            val text = if (detail.value is Gender) {
                itemView.context.getString(GenderTranslation.getByGender(detail.value).translation)
            } else detail.value.toString()
            binding.value = text
            binding.editable = editable
            binding.detail = detail
            if (editable && clickListener != null) {
                binding.setEditClickListener {
                    clickListener.onContactDetailEdit(binding.detail!!)
                }
                binding.setRemoveClickListener {
                    clickListener.onContactDetailRemove(binding.detail!!)
                }
            }
            binding.executePendingBindings()
        }

    }

}

class ContactDetailDiffCallback : DiffUtil.ItemCallback<ContactDetail>() {

    override fun areItemsTheSame(oldItem: ContactDetail, newItem: ContactDetail): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: ContactDetail, newItem: ContactDetail): Boolean {
        return oldItem.equals(newItem)
    }
}

interface ContactDetailClickListener {
    fun onContactDetailEdit(detail: ContactDetail)
    fun onContactDetailRemove(detail: ContactDetail)
}