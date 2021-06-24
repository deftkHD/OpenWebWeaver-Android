package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.forum.IForumPost
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemForumPostBinding
import de.deftk.openww.android.feature.forum.ForumPostIcons
import de.deftk.openww.android.fragments.feature.forum.ForumPostsFragmentDirections

class ForumPostAdapter(private val group: IGroup): ListAdapter<IForumPost, RecyclerView.ViewHolder>(ForumPostDiffCallback()) {

    companion object {

        @JvmStatic
        @BindingAdapter("app:forumPostIcon")
        fun forumPostIcon(view: ImageView, post: IForumPost) {
            view.setImageResource(ForumPostIcons.getByTypeOrDefault(post.icon).resource)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ListItemForumPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ForumPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = getItem(position)
        (holder as ForumPostViewHolder).bind(post, group, null)
    }

    class ForumPostViewHolder(val binding: ListItemForumPostBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener { view ->
                view.findNavController().navigate(ForumPostsFragmentDirections.actionForumPostsFragmentToForumPostFragment(binding.group!!.login, binding.post!!.id, null, view.context.getString(R.string.see_post)))
            }
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