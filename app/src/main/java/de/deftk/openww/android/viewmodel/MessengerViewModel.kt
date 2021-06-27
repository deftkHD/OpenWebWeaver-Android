package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.MessengerRepository
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import de.deftk.openww.api.model.feature.messenger.IQuickMessage
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessengerViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val messengerRepository: MessengerRepository) : ViewModel() {

    //TODO potential use of room to save old messages

    private val _usersResponse: MutableLiveData<Response<List<RemoteScope>>?> = MutableLiveData()
    val usersResponse: LiveData<Response<List<RemoteScope>>?> = _usersResponse

    private val _messagesResponse = MutableLiveData<Response<Pair<List<IQuickMessage>, Boolean>>>()
    val messagesResponse: LiveData<Response<Pair<List<IQuickMessage>, Boolean>>> = _messagesResponse

    private val _addChatResponse = MutableLiveData<Response<RemoteScope>>()
    val addChatResponse: LiveData<Response<RemoteScope>> = _addChatResponse

    private val _removeChatResponse = MutableLiveData<Response<RemoteScope>>()
    val removeChatResponse: LiveData<Response<RemoteScope>> = _removeChatResponse

    private val _sendMessageResponse = MutableLiveData<Response<IQuickMessage>>()
    val sendMessageResponse: LiveData<Response<IQuickMessage>> = _sendMessageResponse

    fun loadChats(apiContext: ApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.getChats(apiContext)
            _usersResponse.value = response
        }
    }

    fun loadHistory(silent: Boolean, apiContext: ApiContext) {
        viewModelScope.launch {
            _messagesResponse.value = messengerRepository.getHistory(apiContext).smartMap { it to silent }
        }
    }

    fun addChat(user: String, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.addChat(user, apiContext)
            if (response is Response.Success && _usersResponse.value is Response.Success) {
                _usersResponse.value = _usersResponse.value?.smartMap { value -> value.toMutableList().apply { add(response.value) } }
            }
            _addChatResponse.value = response
        }
    }

    fun removeChat(user: String, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.removeChat(user, apiContext)
            if (response is Response.Success && _usersResponse.value is Response.Success) {
                _usersResponse.value = _usersResponse.value?.smartMap { value -> value.toMutableList().apply { remove(response.value) } }
            }
            _removeChatResponse.value = response
        }
    }

    fun sendMessage(login: String, text: String?, sessionFile: ISessionFile?, apiContext: ApiContext) {
        viewModelScope.launch {
            _sendMessageResponse.value = messengerRepository.sendMessage(login, sessionFile, text, apiContext)
        }
    }

}