package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import de.deftk.openww.android.databinding.ListItemPermissionBinding
import de.deftk.openww.api.model.Permission

class PermissionAdapter: ListAdapter<Permission, PermissionAdapter.PermissionViewHolder>(PermissionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val binding = ListItemPermissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PermissionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        val permission = getItem(position)
        holder.bind(permission)
    }

    public override fun getItem(position: Int): Permission {
        return super.getItem(position)
    }

    class PermissionViewHolder(val binding: ListItemPermissionBinding): ViewHolder(binding.root) {

        fun bind(permission: Permission) {
            binding.permission = permission
            binding.permissionDescription.text = "" //TODO display description
            binding.executePendingBindings()
        }

    }

}

class PermissionDiffCallback : ItemCallback<Permission>() {

    override fun areItemsTheSame(oldItem: Permission, newItem: Permission): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Permission, newItem: Permission): Boolean {
        return oldItem == newItem
    }
}