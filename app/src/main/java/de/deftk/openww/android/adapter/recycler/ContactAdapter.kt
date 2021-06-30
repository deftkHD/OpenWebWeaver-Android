package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemContactBinding
import de.deftk.openww.android.fragments.feature.contacts.ContactsFragmentDirections
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.contacts.IContact

class ContactAdapter(val scope: IOperatingScope) : ListAdapter<IContact, RecyclerView.ViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val contact = getItem(position)
        (holder as ContactViewHolder).bind(scope, contact)
    }

    public override fun getItem(position: Int): IContact {
        return super.getItem(position)
    }

    class ContactViewHolder(val binding: ListItemContactBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener {
                itemView.findNavController().navigate(ContactsFragmentDirections.actionContactsFragmentToReadContactFragment(binding.scope!!.login, binding.contact!!.id.toString()))
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(scope: IOperatingScope, contact: IContact) {
            binding.scope = scope
            binding.contact = contact
            binding.executePendingBindings()
        }

    }

}

class ContactDiffCallback : DiffUtil.ItemCallback<IContact>() {

    override fun areItemsTheSame(oldItem: IContact, newItem: IContact): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IContact, newItem: IContact): Boolean {
        return false
    }
}