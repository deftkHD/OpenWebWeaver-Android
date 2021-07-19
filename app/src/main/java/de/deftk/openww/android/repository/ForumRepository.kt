package de.deftk.openww.android.repository

import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.forum.ForumPostIcon
import de.deftk.openww.api.model.feature.forum.IForumPost
import javax.inject.Inject

class ForumRepository @Inject constructor() : AbstractRepository() {

    suspend fun getPosts(group: IGroup, parentId: String? = null, apiContext: IApiContext) = apiCall {
        group.getForumPosts(parentId, group.getRequestContext(apiContext)).sortedWith(compareBy ({ it.isPinned }, { it.created.date })).reversed()
    }

    suspend fun addPost(group: IGroup, title: String, text: String, icon: ForumPostIcon, parent: IForumPost? = null, apiContext: IApiContext) = apiCall {
        group.addForumPost(
            title,
            text,
            icon,
            parent,
            null,
            null,
            null,
            group.getRequestContext(apiContext)
        )
    }

    suspend fun deletePost(post: IForumPost, group: IGroup, apiContext: IApiContext) = apiCall {
        post.delete(group.getRequestContext(apiContext))
        post
    }

}