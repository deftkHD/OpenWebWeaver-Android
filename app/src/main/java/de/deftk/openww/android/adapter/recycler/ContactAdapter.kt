package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemContactBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.contacts.IContact

class ContactAdapter(var scope: IOperatingScope, clickListener: ActionModeClickListener<ContactViewHolder>) : ActionModeAdapter<IContact, ContactAdapter.ContactViewHolder>(ContactDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ListItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(scope, contact)
    }

    class ContactViewHolder(val binding: ListItemContactBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeViewHolder(binding.root, clickListener) {

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

        fun bind(scope: IOperatingScope, contact: IContact) {
            binding.scope = scope
            binding.contact = contact
            binding.moreButton.visibility = if (scope.effectiveRights.contains(Permission.ADDRESSES_WRITE) || scope.effectiveRights.contains(Permission.ADDRESSES_ADMIN)) View.VISIBLE else View.INVISIBLE
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