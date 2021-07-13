package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IScope
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.ScopeFilter
import de.deftk.openww.android.repository.GroupRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(private val groupRepository: GroupRepository) : ViewModel() {

    private val allMemberResponses = mutableMapOf<IGroup, MutableLiveData<Response<List<IScope>>>>()

    val filter = MutableLiveData(ScopeFilter())
    val filteredMemberResponses = mutableMapOf<IGroup, LiveData<Response<List<IScope>>>>()

    fun getAllGroupMembers(group: IGroup): LiveData<Response<List<IScope>>> {
        return allMemberResponses.getOrPut(group) { MutableLiveData() }
    }

    fun getFilteredGroupMembers(group: IGroup): LiveData<Response<List<IScope>>> {
        return filteredMemberResponses.getOrPut(group) {
            filter.switchMap { filter ->
                when (filter) {
                    null -> getAllGroupMembers(group)
                    else -> getAllGroupMembers(group).switchMap { response ->
                        val filtered = MutableLiveData<Response<List<IScope>>>()
                        filtered.value = response.smartMap { filter.apply(it) }
                        filtered
                    }
                }
            }
        }
    }

    fun loadMembers(group: IGroup, onlineOnly: Boolean, apiContext: ApiContext) {
        viewModelScope.launch {
            (getAllGroupMembers(group) as MutableLiveData).value = groupRepository.getMembers(group, onlineOnly, apiContext)
        }
    }

}