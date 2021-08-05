package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.MessageFilter
import de.deftk.openww.android.filter.ScopeFilter
import de.deftk.openww.android.repository.MessengerRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IScope
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import de.deftk.openww.api.model.feature.messenger.IQuickMessage
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessengerViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val messengerRepository: MessengerRepository) : ViewModel() {

    private val _usersResponse: MutableLiveData<Response<List<RemoteScope>>> = MutableLiveData()
    val allUsersResponse: LiveData<Response<List<RemoteScope>>> = _usersResponse

    val userFilter = MutableLiveData(ScopeFilter())
    val filteredUsersResponse: LiveData<Response<List<RemoteScope>>>
        get() = userFilter.switchMap { filter ->
            when (filter) {
                null -> allUsersResponse
                else -> allUsersResponse.switchMap { response ->
                    val filtered = MutableLiveData<Response<List<RemoteScope>>>()
                    filtered.value = response.smartMap { filter.apply(it) as List<RemoteScope> }
                    filtered
                }
            }
        }

    private val _messagesResponse = mutableMapOf<String, MutableLiveData<Response<Pair<List<IQuickMessage>, Boolean>>>>()
    //private val allMessagesResponses: Map<String, LiveData<Response<Pair<List<IQuickMessage>, Boolean>>>> = _messagesResponse

    val messageFilter = MutableLiveData(MessageFilter())
    private val filteredMessageResponses = mutableMapOf<String, LiveData<Response<Pair<List<IQuickMessage>, Boolean>>>>()

    private val _addChatResponse = MutableLiveData<Response<RemoteScope>?>()
    val addChatResponse: LiveData<Response<RemoteScope>?> = _addChatResponse

    private val _removeChatResponse = MutableLiveData<Response<RemoteScope>?>()
    val removeChatResponse: LiveData<Response<RemoteScope>?> = _removeChatResponse

    private val _sendMessageResponse = MutableLiveData<Response<IQuickMessage>>()
    val sendMessageResponse: LiveData<Response<IQuickMessage>> = _sendMessageResponse

    private val _batchDeleteResponse = MutableLiveData<List<Response<IScope>>?>()
    val batchDeleteResponse: LiveData<List<Response<IScope>>?> = _batchDeleteResponse

    fun getAllMessagesResponse(with: String): LiveData<Response<Pair<List<IQuickMessage>, Boolean>>>  {
        return _messagesResponse.getOrPut(with) { MutableLiveData() }
    }

    fun getFilteredMessagesResponse(with: String): LiveData<Response<Pair<List<IQuickMessage>, Boolean>>> {
        return filteredMessageResponses.getOrPut(with) {
            messageFilter.switchMap { filter ->
                when (filter) {
                    null -> getAllMessagesResponse(with)
                    else -> getAllMessagesResponse(with).switchMap { response ->
                        val filtered = MutableLiveData<Response<Pair<List<IQuickMessage>, Boolean>>>()
                        filtered.value = response.smartMap { filter.apply(it.first) to it.second }
                        filtered
                    }
                }
            }
        }
    }

    fun loadChats(apiContext: IApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.getChats(apiContext)
            _usersResponse.value = response
        }
    }

    fun loadHistory(with: String, silent: Boolean, apiContext: IApiContext) {
        viewModelScope.launch {
            _messagesResponse.getOrPut(with) { MutableLiveData() }.value = messengerRepository.getHistory(with, apiContext).smartMap { it to silent }
        }
    }

    fun addChat(user: String, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.addChat(user, apiContext)
            _addChatResponse.value = response
            if (response is Response.Success && _usersResponse.value is Response.Success) {
                _usersResponse.value = _usersResponse.value?.smartMap { value -> value.toMutableList().apply { add(response.value) } }
            }
        }
    }

    fun removeChat(user: String, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = messengerRepository.removeChat(user, apiContext)
            _removeChatResponse.value = response
            if (response is Response.Success && _usersResponse.value is Response.Success) {
                _usersResponse.value = _usersResponse.value?.smartMap { value -> value.toMutableList().apply { remove(response.value) } }
            }
        }
    }

    fun sendMessage(login: String, text: String?, sessionFile: ISessionFile?, apiContext: IApiContext) {
        viewModelScope.launch {
            _sendMessageResponse.value = messengerRepository.sendMessage(login, sessionFile, text, apiContext)
        }
    }

    fun clearChat(user: String, apiContext: IApiContext) {
        viewModelScope.launch {
            messengerRepository.clearChat(user, apiContext)
            _messagesResponse.getOrPut(user) { MutableLiveData() }.value = Response.Success(Pair(emptyList(), false))
        }
    }

    fun batchDelete(selectedTasks: List<IScope>, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = selectedTasks.map { messengerRepository.removeChat(it.login, apiContext) }
            _batchDeleteResponse.value = responses
            val tasks = allUsersResponse.value?.valueOrNull()
            if (tasks != null) {
                val currentTasks = tasks.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentTasks.remove(response.value)
                    }
                }
                _usersResponse.value = Response.Success(currentTasks)
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }

    fun resetAddChatResponse() {
        _addChatResponse.value = null
    }

    fun resetRemoveChatResponse() {
        _removeChatResponse.value = null
    }

}