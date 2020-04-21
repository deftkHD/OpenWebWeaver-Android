package de.deftk.lonet.mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.feature.forum.ForumPost
import de.deftk.lonet.mobile.R
import java.text.DateFormat

class ForumPostAdapter(context: Context, elements: List<ForumPost>): ArrayAdapter<ForumPost>(context, 0, elements) {

    companion object {
        val postIconMap = mapOf(
            Pair(ForumPost.ForumMessageIcon.INFORMATION, R.drawable.ic_forum_post_information),
            Pair(ForumPost.ForumMessageIcon.HUMOR, R.drawable.ic_forum_post_happy),
            Pair(ForumPost.ForumMessageIcon.QUESTION, R.drawable.ic_forum_post_question),
            Pair(ForumPost.ForumMessageIcon.ANSWER, R.drawable.ic_forum_post_answer),
            Pair(ForumPost.ForumMessageIcon.UP_VOTE, R.drawable.ic_forum_post_upvote),
            Pair(ForumPost.ForumMessageIcon.DOWN_VOTE, R.drawable.ic_forum_post_downvote)
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_forum_post, parent, false)
        val item = getItem(position) ?: return listItemView
        listItemView.findViewById<ImageView>(R.id.forum_post_image).setImageResource(postIconMap[item.icon] ?: R.drawable.ic_forum_post_unknown)
        listItemView.findViewById<TextView>(R.id.forum_post_title).text = item.title
        listItemView.findViewById<TextView>(R.id.forum_post_author).text = item.creationMember.name ?: item.creationMember.login
        listItemView.findViewById<TextView>(R.id.forum_post_date).text = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(item.creationDate)
        return listItemView
    }

}