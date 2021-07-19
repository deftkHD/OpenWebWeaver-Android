package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.ForumPostFilter
import de.deftk.openww.android.repository.ForumRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.forum.IForumPost
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val forumRepository: ForumRepository) : ViewModel() {

    private val postsResponses = mutableMapOf<IGroup, MutableLiveData<Response<List<IForumPost>>>>()

    val filter = MutableLiveData(ForumPostFilter())
    private val filteredPostResponses = mutableMapOf<IGroup, LiveData<Response<List<IForumPost>>>>()

    private val _deleteResponse = MutableLiveData<Response<IForumPost>?>()
    val deleteResponse: LiveData<Response<IForumPost>?> = _deleteResponse

    private val _batchDeleteResponse = MutableLiveData<List<Response<IForumPost>>?>()
    val batchDeleteResponse: LiveData<List<Response<IForumPost>>?> = _batchDeleteResponse

    fun getAllForumPosts(group: IGroup): LiveData<Response<List<IForumPost>>> {
        return postsResponses.getOrPut(group) { MutableLiveData() }
    }

    fun getFilteredForumPosts(group: IGroup): LiveData<Response<List<IForumPost>>> {
        return filteredPostResponses.getOrPut(group) {
            filter.switchMap { filter ->
                when (filter) {
                    null -> getAllForumPosts(group)
                    else -> getAllForumPosts(group).switchMap { response ->
                        val filtered = MutableLiveData<Response<List<IForumPost>>>()
                        filtered.value = response.smartMap { filter.apply(it) }
                        filtered
                    }
                }
            }
        }
    }

    fun loadForumPosts(group: IGroup, parentId: String? = null, apiContext: IApiContext) {
        viewModelScope.launch {
            (getAllForumPosts(group) as MutableLiveData).value = forumRepository.getPosts(group, parentId, apiContext)
        }
    }

    fun filterRootPosts(allPosts: List<IForumPost>): List<IForumPost> {
        return allPosts.filter { it.parentId == "0" }
    }

    fun getComments(group: IGroup, postId: String): List<IForumPost> {
        return postsResponses[group]?.value?.valueOrNull()?.filter { it.parentId == postId } ?: emptyList()
    }

    fun deletePost(post: IForumPost, parent: IForumPost?, group: IGroup, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = forumRepository.deletePost(post, group, apiContext)
            _deleteResponse.value = response
            if (response is Response.Success) {
                parent?.getComments()?.toMutableList()?.remove(post)
                val liveData = (getAllForumPosts(group) as MutableLiveData)
                val posts = liveData.value?.valueOrNull()?.toMutableList()
                if (posts != null) {
                    deletePostRecursiveLocally(posts, post)
                    liveData.value = Response.Success(posts)
                }
            }
        }
    }

    private fun deletePostRecursiveLocally(posts: MutableList<IForumPost>, post: IForumPost) {
        val children = posts.filter { it.parentId == post.id }
        children.forEach { child ->
            deletePostRecursiveLocally(posts, child)
        }
        posts.remove(post)
    }

    fun resetDeleteResponse() {
        _deleteResponse.value = null
    }

    fun getParentPost(rootPosts: List<IForumPost>, path: MutableList<String>): IForumPost? {
        val id = path.lastOrNull() ?: return null
        return findPostOrComment(rootPosts, id)
    }

    fun findPostOrComment(rootPosts: List<IForumPost>, id: String): IForumPost? {
        return rootPosts.firstOrNull { it.id == id }
    }

    fun batchDelete(selectedPosts: List<IForumPost>, group: IGroup, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = selectedPosts.map { forumRepository.deletePost(it, group, apiContext) }
            _batchDeleteResponse.value = responses
            responses.forEach { response ->
                if (response is Response.Success) {
                    val liveData = postsResponses[group]
                    if (liveData?.value is Response.Success) {
                        val posts = (liveData.value!! as Response.Success).value.toMutableList()
                        deletePostRecursiveLocally(posts, response.value)
                        liveData.value = Response.Success(posts)
                    }
                }
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }

}