package de.deftk.openww.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IScope
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.GroupRepository
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