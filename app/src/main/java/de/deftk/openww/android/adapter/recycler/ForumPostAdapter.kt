package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import de.deftk.openww.android.databinding.ListItemForumPostBinding
import de.deftk.openww.android.fragments.ActionModeClickListener
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.forum.IForumPost

class ForumPostAdapter(private val group: IGroup, clickListener: ActionModeClickListener<ForumPostViewHolder>): ActionModeAdapter<IForumPost, ForumPostAdapter.ForumPostViewHolder>(ForumPostDiffCallback(), clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumPostViewHolder {
        val binding = ListItemForumPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ForumPostViewHolder(binding, clickListener as ActionModeClickListener<ActionModeViewHolder>)
    }

    override fun onBindViewHolder(holder: ForumPostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post, group, null)
    }

    class ForumPostViewHolder(val binding: ListItemForumPostBinding, clickListener: ActionModeClickListener<ActionModeViewHolder>) : ActionModeViewHolder(binding.root, clickListener) {

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

        fun bind(post: IForumPost, group: IGroup, parentIds: Array<String>?) {
            binding.post = post
            binding.group = group
            binding.parentIds = parentIds
            binding.executePendingBindings()
        }

    }

}

class ForumPostDiffCallback: DiffUtil.ItemCallback<IForumPost>() {

    override fun areItemsTheSame(oldItem: IForumPost, newItem: IForumPost): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IForumPost, newItem: IForumPost): Boolean {
        return oldItem.equals(newItem)
    }
}