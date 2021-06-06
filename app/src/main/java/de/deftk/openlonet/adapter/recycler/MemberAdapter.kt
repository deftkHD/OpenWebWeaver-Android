package de.deftk.openlonet.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.lonet.api.model.IScope
import de.deftk.lonet.api.model.RemoteScope
import de.deftk.openlonet.R
import de.deftk.openlonet.databinding.ListItemMemberBinding

class MemberAdapter: ListAdapter<IScope, RecyclerView.ViewHolder>(MemberDiffCallback()) {

    companion object {

        @JvmStatic
        @BindingAdapter("app:memberOnlineImage")
        fun memberOnlineImage(view: ImageView, scope: IScope) {
            if (scope is RemoteScope) {
                if (scope.isOnline) {
                    view.setImageResource(R.drawable.ic_person_orange_24)
                } else {
                    view.setImageResource(R.drawable.ic_person_24)
                }
            } else {
                view.setImageResource(R.drawable.ic_person_orange_24)
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
            } else {
                view.setText(R.string.online)
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
            itemView.setOnClickListener {
                itemView.showContextMenu()
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