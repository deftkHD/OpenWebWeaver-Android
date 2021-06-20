package de.deftk.openlonet.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.lonet.api.model.IGroup
import de.deftk.openlonet.databinding.ListItemForumBinding
import de.deftk.openlonet.fragments.feature.forum.ForumGroupFragmentDirections

class ForumGroupAdapter : ListAdapter<IGroup, ForumGroupAdapter.ForumGroupViewHolder>(ForumGroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumGroupViewHolder {
        val binding = ListItemForumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ForumGroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForumGroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ForumGroupViewHolder(val binding: ListItemForumBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(ForumGroupFragmentDirections.actionForumGroupFragmentToForumPostsFragment(binding.group!!.login, binding.group!!.name))
            }
        }

        fun bind(group: IGroup) {
            binding.group = group
            binding.executePendingBindings()
        }

    }

}

class ForumGroupDiffCallback: DiffUtil.ItemCallback<IGroup>() {

    override fun areItemsTheSame(oldItem: IGroup, newItem: IGroup): Boolean {
        return oldItem.login == newItem.login
    }

    override fun areContentsTheSame(oldItem: IGroup, newItem: IGroup): Boolean {
        return oldItem.equals(newItem)
    }
}