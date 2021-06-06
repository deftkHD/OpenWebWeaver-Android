package de.deftk.openlonet.adapter

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.feature.forum.IForumPost
import de.deftk.openlonet.R
import de.deftk.openlonet.fragments.feature.forum.ForumPostFragmentDirections
import de.deftk.openlonet.utils.TextUtils

class ForumPostCommentRecyclerAdapter(private val comments: List<IForumPost>, private val group: IGroup, private val navController: NavController, private val parentPostIds: Array<String>, private val postId: String): RecyclerView.Adapter<ForumPostCommentRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_forum_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.commentImage.setImageResource(ForumPostAdapter.postIconMap[comment.icon] ?: R.drawable.ic_help_24)
        holder.commentTitle.text = comment.title
        holder.commentAuthor.text = comment.created.member.name
        holder.commentDate.text = TextUtils.parseShortDate(comment.created.date)
        holder.commentText.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(comment.text)))
        holder.commentText.movementMethod = LinkMovementMethod.getInstance()
        holder.showComments.isVisible = comment.getComments().isNotEmpty()
        if (comment.getComments().isNotEmpty()) {
            holder.showComments.setOnClickListener {
                val action = ForumPostFragmentDirections.actionForumPostFragmentSelf(group.login, comment.id, arrayOf(*parentPostIds, postId), holder.itemView.context.getString(R.string.see_comment))
                navController.navigate(action)
            }
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    class ViewHolder(rootView: View): RecyclerView.ViewHolder(rootView) {
        val commentImage = rootView.findViewById<ImageView>(R.id.forum_comment_image)!!
        val commentTitle = rootView.findViewById<TextView>(R.id.forum_comment_title)!!
        val commentDate = rootView.findViewById<TextView>(R.id.forum_comment_date)!!
        val commentAuthor = rootView.findViewById<TextView>(R.id.forum_comment_author)!!
        val commentText = rootView.findViewById<TextView>(R.id.forum_comment_text)!!
        val showComments = rootView.findViewById<TextView>(R.id.forum_comment_show_comments)!!
    }

}