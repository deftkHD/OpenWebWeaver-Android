package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IScope
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemMemberBinding
import de.deftk.openww.android.fragments.feature.members.MembersGroupsFragmentDirections

class MemberAdapter: ListAdapter<IScope, RecyclerView.ViewHolder>(MemberDiffCallback()) {

    companion object {

        @JvmStatic
        @BindingAdapter("app:memberOnlineImage")
        fun memberOnlineImage(view: ImageView, scope: IScope) {
            if (scope is RemoteScope) {
                if (scope.isOnline) {
                    view.setImageResource(R.drawable.ic_person_accent_24)
                } else {
                    view.setImageResource(R.drawable.ic_person_24)
                }
            } else if (scope is IUser) {
                view.setImageResource(R.drawable.ic_person_accent_24)
            } else {
                view.setImageResource(R.drawable.ic_person_24)
            }
        }

        @JvmStatic
        @BindingAdapter("app:memberOnlineText")
        fun memberOnlineText(view: TextView, scope: IScope) {
            if (scope is RemoteScope) {
                if (scope.isOnline) {
                    view.setText(R.string.online)
                } else {
                    view.setText(R.string.offline)
                }
            } else if (scope is IUser) {
                view.setText(R.string.online)
            } else {
                view.isVisible = false
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val scope = getItem(position)
        (holder as MemberViewHolder).bind(scope)
    }

    public override fun getItem(position: Int): IScope {
        return super.getItem(position)
    }

    class MemberViewHolder(val binding: ListItemMemberBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener {
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