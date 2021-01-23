package de.deftk.openlonet.adapter

import android.content.Intent
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.deftk.lonet.api.implementation.Group
import de.deftk.lonet.api.implementation.feature.forum.ForumPost
import de.deftk.lonet.api.model.feature.forum.IForumPost
import de.deftk.openlonet.R
import de.deftk.openlonet.activities.feature.forum.ForumPostActivity
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.putJsonExtra

class ForumPostCommentRecyclerAdapter(private val comments: List<IForumPost>, private val group: Group): RecyclerView.Adapter<ForumPostCommentRecyclerAdapter.ViewHolder>() {

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
                val context = holder.itemView.context
                val intent = Intent(context, ForumPostActivity::class.java)
                intent.putJsonExtra(ForumPostActivity.EXTRA_FORUM_POST, comment as ForumPost)
                intent.putJsonExtra(ForumPostActivity.EXTRA_GROUP, group)
                context.startActivity(intent)
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