package de.deftk.openlonet.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.IScope
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.repository.GroupRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(private val groupRepository: GroupRepository) : ViewModel() {

    private val memberResponses = mutableMapOf<IGroup, MutableLiveData<Response<List<IScope>>>>()

    fun getGroupMembers(group: IGroup): LiveData<Response<List<IScope>>> {
        return memberResponses.getOrPut(group) { MutableLiveData() }
    }

    fun loadMembers(group: IGroup, onlineOnly: Boolean, apiContext: ApiContext) {
        viewModelScope.launch {
            (getGroupMembers(group) as MutableLiveData).value = groupRepository.getMembers(group, onlineOnly, apiContext)
        }
    }

}