package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.forum.IForumPost
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.ForumRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val forumRepository: ForumRepository) : ViewModel() {

    private val memberResponses = mutableMapOf<IGroup, MutableLiveData<Response<List<IForumPost>>>>()

    fun getForumPosts(group: IGroup): LiveData<Response<List<IForumPost>>> {
        return memberResponses.getOrPut(group) { MutableLiveData() }
    }

    fun loadForumPosts(group: IGroup, parentId: String? = null, apiContext: ApiContext) {
        viewModelScope.launch {
            (getForumPosts(group) as MutableLiveData).value = forumRepository.getPosts(group, parentId, apiContext)
        }
    }

    fun findPostOrComment(rootPosts: List<IForumPost>, path: MutableList<String>?, id: String): IForumPost? {
        var posts = rootPosts

        while (true) {
            if (path == null || path.isEmpty()) {
                return posts.firstOrNull { it.id == id }
            } else {
                val root = posts.firstOrNull { it.id == path[0] }
                if (root != null) {
                    path.removeFirst()
                    posts = root.getComments()
                    // repeat loop
                } else {
                    // path segment not found
                    return null
                }
            }
        }
    }

}