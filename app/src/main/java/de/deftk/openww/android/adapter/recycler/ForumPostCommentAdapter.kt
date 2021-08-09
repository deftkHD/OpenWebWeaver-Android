package de.deftk.openww.android.adapter.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.deftk.openww.android.R
import de.deftk.openww.android.databinding.ListItemForumCommentBinding
import de.deftk.openww.android.fragments.feature.forum.ForumPostFragmentDirections
import de.deftk.openww.android.viewmodel.ForumViewModel
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.forum.IForumPost

class ForumPostCommentAdapter(var group: IGroup, private val path: Array<String>, private val forumViewModel: ForumViewModel, private val navController: NavController): ListAdapter<IForumPost, ForumPostCommentAdapter.CommentViewHolder>(ForumPostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ListItemForumCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        val children = forumViewModel.getComments(group, comment.id)
        holder.bind(comment, group, path, children.isNotEmpty(), navController)
    }

    public override fun getItem(position: Int): IForumPost {
        return super.getItem(position)
    }

    class CommentViewHolder(val binding: ListItemForumCommentBinding): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setMenuClickListener {
                itemView.showContextMenu()
            }
        }

        fun bind(post: IForumPost, group: IGroup, path: Array<String>, hasChildren: Boolean, navController: NavController) {
            binding.post = post
            binding.group = group
            binding.hasChildren = hasChildren
            binding.navController = navController
            binding.moreButton.visibility = if (group.effectiveRights.contains(Permission.FORUM_WRITE) || group.effectiveRights.contains(Permission.FORUM_ADMIN)) View.VISIBLE else View.INVISIBLE
            binding.setShowMoreClickListener { view ->
                val action = ForumPostFragmentDirections.actionForumPostFragmentSelf(group.login, post.id, path, view.context.getString(R.string.see_comment))
                view.findNavController().navigate(action)
            }
            binding.executePendingBindings()
        }

    }

}