package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemRequestBinding
import de.deftk.openww.android.feature.devtools.PastRequest
import de.deftk.openww.android.fragments.ActionModeClickListener

// maybe this whole thing could receive an actionmode menu, but until now I just implemented it this way because its easier
class RequestAdapter(clickListener: ActionModeClickListener<RequestViewHolder>): ActionModeAdapter<PastRequest, RequestAdapter.RequestViewHolder>(RequestDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ListItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        @Suppress("UNCHECKED_CAST")
        return RequestViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = getItem(position)
        holder.bind(request)
    }

    class RequestViewHolder(val binding: ListItemRequestBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>): ActionModeAdapter.ActionModeViewHolder(binding.root, clickListener) {

        private var selected = false

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

        fun bind(pastRequest: PastRequest) {
            val title = pastRequest.getTitle()
            val summary = pastRequest.getRequestSummary()
            binding.title = title
            binding.details = summary.joinToString(", ")
            binding.requestId = pastRequest.id
            binding.executePendingBindings()
        }
    }

}

class RequestDiffCallback : DiffUtil.ItemCallback<PastRequest>() {

    override fun areItemsTheSame(oldItem: PastRequest, newItem: PastRequest): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PastRequest, newItem: PastRequest): Boolean {
        return oldItem.equals(newItem)
    }
}