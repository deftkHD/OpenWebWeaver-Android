package de.deftk.openlonet.repository

import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.feature.forum.ForumPostIcon
import de.deftk.lonet.api.model.feature.forum.IForumPost
import javax.inject.Inject

class ForumRepository @Inject constructor() : AbstractRepository() {

    suspend fun getPosts(group: IGroup, parentId: String? = null, apiContext: ApiContext) = apiCall {
        group.getForumPosts(parentId, group.getRequestContext(apiContext)).sortedWith(compareBy ({ it.isPinned }, { it.created.date })).reversed()
    }

    suspend fun addPost(group: IGroup, title: String, text: String, icon: ForumPostIcon, parent: IForumPost? = null, apiContext: ApiContext) = apiCall {
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

}