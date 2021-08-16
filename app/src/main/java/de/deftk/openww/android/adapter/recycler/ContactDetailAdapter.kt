package de.deftk.openww.android.adapter.recycler

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemContactDetailBinding
import de.deftk.openww.android.feature.contacts.ContactDetail
import de.deftk.openww.android.feature.contacts.ContactDetailType
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
            binding.setClickListener {
                when (detail.type) {
                    ContactDetailType.HOME_PHONE -> phoneCallIntent(detail.value.toString(), itemView.context)
                    ContactDetailType.MOBILE_PHONE -> phoneCallIntent(detail.value.toString(), itemView.context)
                    ContactDetailType.BUSINESS_PHONE -> phoneCallIntent(detail.value.toString(), itemView.context)
                    ContactDetailType.HOME_FAX -> phoneCallIntent(detail.value.toString(), itemView.context)
                    ContactDetailType.BUSINESS_FAX -> phoneCallIntent(detail.value.toString(), itemView.context)
                    ContactDetailType.EMAIL_ADDRESS -> sendEmailIntent(detail.value.toString(), itemView.context)
                    ContactDetailType.EMAIL_ADDRESS_2 -> sendEmailIntent(detail.value.toString(), itemView.context)
                    ContactDetailType.EMAIL_ADDRESS_3 -> sendEmailIntent(detail.value.toString(), itemView.context)
                    else -> copyDetailValue(detail, itemView.context)
                }
            }
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

        private fun sendEmailIntent(address: String, context: Context) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(address)}"))
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_mail)))
        }

        private fun phoneCallIntent(number: String, context: Context) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.call)))
        }

        private fun copyDetailValue(detail: ContactDetail, context: Context) {
            val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(detail.type.name, detail.value.toString())
            clipBoard.setPrimaryClip(clip)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
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