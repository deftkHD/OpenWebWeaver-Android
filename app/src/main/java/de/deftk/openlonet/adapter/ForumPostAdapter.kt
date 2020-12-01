package de.deftk.openlonet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.deftk.lonet.api.model.feature.forum.ForumPost
import de.deftk.lonet.api.model.feature.forum.ForumPostIcon
import de.deftk.openlonet.R
import de.deftk.openlonet.utils.TextUtils
import de.deftk.openlonet.utils.filter.FilterableAdapter
import de.deftk.openlonet.utils.filter.filterApplies

class ForumPostAdapter(context: Context, elements: List<ForumPost>): FilterableAdapter<ForumPost>(context, elements) {

    companion object {
        val postIconMap = mapOf(
            Pair(ForumPostIcon.INFORMATION, R.drawable.ic_info_24),
            Pair(ForumPostIcon.HUMOR, R.drawable.ic_face_24),
            Pair(ForumPostIcon.QUESTION, R.drawable.ic_help_outline_24),
            Pair(ForumPostIcon.ANSWER, R.drawable.ic_chat_24),
            Pair(ForumPostIcon.UP_VOTE, R.drawable.ic_thumbs_up_24),
            Pair(ForumPostIcon.DOWN_VOTE, R.drawable.ic_thumbs_down_24)
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_forum_post, parent, false)
        val item = getItem(position) ?: return listItemView
        listItemView.findViewById<ImageView>(R.id.forum_post_image).setImageResource(postIconMap[item.icon] ?: R.drawable.ic_help_24)
        listItemView.findViewById<TextView>(R.id.forum_post_title).text = item.title
        listItemView.findViewById<TextView>(R.id.forum_post_author).text = item.creationMember.getName()
        listItemView.findViewById<TextView>(R.id.forum_post_date).text = TextUtils.parseShortDate(item.creationDate)
        listItemView.findViewById<ImageView>(R.id.forum_post_locked).visibility = if (item.locked == true) View.VISIBLE else View.INVISIBLE
        listItemView.findViewById<ImageView>(R.id.forum_post_pinned).visibility = if (item.pinned == true) View.VISIBLE else View.INVISIBLE
        return listItemView
    }

    override fun search(constraint: String?): List<ForumPost> {
        if (constraint == null)
            return originalElements
        return originalElements.filter {
            it.title.filterApplies(constraint)
                    || it.text.filterApplies(constraint)
                    || it.creationMember.filterApplies(constraint)
        }
    }

    override fun sort(elements: List<ForumPost>): List<ForumPost> {
        return elements.sortedWith(compareBy({ it.pinned }, { it.creationDate })).reversed()
    }
}