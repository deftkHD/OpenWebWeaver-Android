package de.deftk.openww.android.feature.forum

import androidx.annotation.DrawableRes
import de.deftk.openww.api.model.feature.forum.ForumPostIcon
import de.deftk.openww.android.R

enum class ForumPostIcons(val type: ForumPostIcon, @DrawableRes val resource: Int) {

    INFORMATION(ForumPostIcon.INFORMATION, R.drawable.ic_info_24),
    HUMOR(ForumPostIcon.HUMOR, R.drawable.ic_face_24),
    QUESTION(ForumPostIcon.QUESTION, R.drawable.ic_help_outline_24),
    ANSWER(ForumPostIcon.ANSWER, R.drawable.ic_chat_24),
    UPVOTE(ForumPostIcon.UPVOTE, R.drawable.ic_thumbs_up_24),
    DOWNVOTE(ForumPostIcon.DOWNVOTE, R.drawable.ic_thumbs_down_24);

    companion object {
        @JvmStatic
        fun getByType(type: ForumPostIcon?): ForumPostIcons? {
            if (type == null) return null
            return values().firstOrNull { it.type == type }
        }

        @JvmStatic
        fun getByTypeOrDefault(type: ForumPostIcon?): ForumPostIcons {
            return getByType(type) ?: INFORMATION
        }
    }

}