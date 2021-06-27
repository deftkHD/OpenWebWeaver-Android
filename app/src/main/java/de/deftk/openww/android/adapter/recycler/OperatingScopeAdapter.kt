package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.databinding.ListItemScopeBinding
import de.deftk.openww.android.fragments.IOperatingScopeClickListener
import de.deftk.openww.api.model.IOperatingScope

class OperatingScopeAdapter(private val clickListener: IOperatingScopeClickListener) : ListAdapter<IOperatingScope, RecyclerView.ViewHolder>(OperatingScopeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemScopeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OperatingScopeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val scope = getItem(position)
        (holder as OperatingScopeViewHolder).bind(scope, clickListener)
    }

    public override fun getItem(position: Int): IOperatingScope {
        return super.getItem(position)
    }

    class OperatingScopeViewHolder(val binding: ListItemScopeBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnLongClickListener {
                itemView.showContextMenu()
                true
            }
        }

        fun bind(scope: IOperatingScope, clickListener: IOperatingScopeClickListener) {
            binding.scope = scope
            binding.setClickListener {
                clickListener.onOperatingScopeClicked(scope)
            }
            binding.executePendingBindings()
        }

    }
}

class OperatingScopeDiffCallback: DiffUtil.ItemCallback<IOperatingScope>() {

    override fun areItemsTheSame(oldItem: IOperatingScope, newItem: IOperatingScope): Boolean {
        return oldItem.login == newItem.login
    }

    override fun areContentsTheSame(oldItem: IOperatingScope, newItem: IOperatingScope): Boolean {
        return oldItem.login == newItem.login
    }
}