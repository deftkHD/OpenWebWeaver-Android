package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemScopeBinding
import de.deftk.openww.android.fragments.feature.members.MembersGroupsFragmentDirections
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IScope

class MemberAdapter: ListAdapter<IScope, RecyclerView.ViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemScopeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val scope = getItem(position)
        (holder as MemberViewHolder).bind(scope)
    }

    public override fun getItem(position: Int): IScope {
        return super.getItem(position)
    }

    class MemberViewHolder(val binding: ListItemScopeBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setMenuClickListener {
                itemView.showContextMenu()
            }
            itemView.setOnClickListener {
                if (binding.scope is IGroup) {
                    itemView.findNavController().navigate(MembersGroupsFragmentDirections.actionMembersGroupFragmentToMembersFragment(binding.scope!!.login, binding.scope!!.name))
                } else {
                    itemView.showContextMenu()
                }
            }
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(scope: IScope) {
            binding.scope = scope
            binding.executePendingBindings()
        }

    }
}

class MemberDiffCallback: DiffUtil.ItemCallback<IScope>() {

    override fun areItemsTheSame(oldItem: IScope, newItem: IScope): Boolean {
        return oldItem.login == newItem.login
    }

    override fun areContentsTheSame(oldItem: IScope, newItem: IScope): Boolean {
        return oldItem.login == newItem.login
    }
}