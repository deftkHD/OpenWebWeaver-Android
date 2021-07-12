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

    private val _usersResponse: MutableLiveData<Response<List<RemoteScope>>?> = MutableLiveData()
    val usersResponse: LiveData<Response<List<RemoteScope>>?> = _usersResponse

    private val _messagesResponse = mutableMapOf<String, MutableLiveData<Response<Pair<List<IQuickMessage>, Boolean>>>>()
    val messagesResponse: Map<String, LiveData<Response<Pair<List<IQuickMessage>, Boolean>>>> = _messagesResponse

    private val _addChatResponse = MutableLiveData<Response<RemoteScope>>()
    val addChatResponse: LiveData<Response<RemoteScope>> = _addChatResponse

    private val _removeChatResponse = MutableLiveData<Response<RemoteScope>>()
    val removeChatResponse: LiveData<Response<RemoteScope>> = _removeChatResponse

    private val _sendMessageResponse = MutableLiveData<Response<IQuickMessage>>()
    val sendMessageResponse: LiveData<Response<IQuickMessage>> = _sendMessageResponse

    fun getChatLiveData(with: String): LiveData<Response<Pair<List<IQuickMessage>, Boolean>>>  {
        return _messagesResponse.getOrPut(with) { MutableLiveData() }
    }

    fun loadChats(apiContext: ApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.getChats(apiContext)
            _usersResponse.value = response
        }
    }

    fun loadHistory(with: String, silent: Boolean, apiContext: ApiContext) {
        viewModelScope.launch {
            _messagesResponse.getOrPut(with) { MutableLiveData() }.value = messengerRepository.getHistory(with, apiContext).smartMap { it to silent }
        }
    }

    fun addChat(user: String, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.addChat(user, apiContext)
            _addChatResponse.value = response
            if (response is Response.Success && _usersResponse.value is Response.Success) {
                _usersResponse.value = _usersResponse.value?.smartMap { value -> value.toMutableList().apply { add(response.value) } }
            }
        }
    }

    fun removeChat(user: String, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.removeChat(user, apiContext)
            _removeChatResponse.value = response
            if (response is Response.Success && _usersResponse.value is Response.Success) {
                _usersResponse.value = _usersResponse.value?.smartMap { value -> value.toMutableList().apply { remove(response.value) } }
            }
        }
    }

    fun sendMessage(login: String, text: String?, sessionFile: ISessionFile?, apiContext: ApiContext) {
        viewModelScope.launch {
            _sendMessageResponse.value = messengerRepository.sendMessage(login, sessionFile, text, apiContext)
        }
    }

    fun clearChat(user: String) {
        viewModelScope.launch {
            messengerRepository.clearChat(user)
            _messagesResponse.getOrPut(user) { MutableLiveData() }.value = Response.Success(Pair(emptyList(), false))
        }
    }

}