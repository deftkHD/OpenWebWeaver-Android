package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.BoardRepository
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import de.deftk.openww.api.model.feature.board.IBoardNotification
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BoardViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val boardRepository: BoardRepository): ViewModel() {

    private val _notificationsResponse = MutableLiveData<Response<List<Pair<IBoardNotification, IGroup>>>>()
    val notificationsResponse: LiveData<Response<List<Pair<IBoardNotification, IGroup>>>> = _notificationsResponse

    private val _postResponse = MutableLiveData<Response<IBoardNotification?>?>()
    val postResponse: LiveData<Response<IBoardNotification?>?> = _postResponse

    private val _batchDeleteResponse = MutableLiveData<List<Response<Pair<IBoardNotification, IGroup>>>?>()
    val batchDeleteResponse: LiveData<List<Response<Pair<IBoardNotification, IGroup>>>?> = _batchDeleteResponse

    fun setSearchText(query: String?) {
        savedStateHandle["query"] = query
    }

    fun loadBoardNotifications(apiContext: ApiContext) {
        viewModelScope.launch {
            _notificationsResponse.value = boardRepository.getBoardNotifications(apiContext)
        }
    }

    fun addBoardNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = boardRepository.addBoardNotification(
                title,
                text,
                color,
                killDate,
                group,
                apiContext
            )
            _postResponse.value = response
            if (_notificationsResponse.value is Response.Success && response is Response.Success) {
                // inject new notification into stored livedata
                val notifications = (_notificationsResponse.value as Response.Success<List<Pair<IBoardNotification, IGroup>>>).value.toMutableList()
                notifications.add(response.value to group)
                _notificationsResponse.value = Response.Success(notifications)
            }
        }
    }

    fun editBoardNotification(notification: IBoardNotification, title: String, text: String, color: BoardNotificationColor, killDate: Date?, group: IGroup, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = boardRepository.editBoardNotification(
                notification,
                title,
                text,
                color,
                killDate,
                BoardType.ALL,
                group.getRequestContext(apiContext)
            )
            _postResponse.value = response.smartMap { notification }
            if (response is Response.Success) {
                // no need to update items in list because the instance will be the same

                // trigger observers
                _notificationsResponse.value = _notificationsResponse.value
            }
        }
    }

    fun deleteBoardNotification(notification: IBoardNotification, group: IGroup, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = boardRepository.deleteBoardNotification(notification, group, apiContext)
            _postResponse.value = Response.Success(notification)
            if (response is Response.Success && _notificationsResponse.value is Response.Success) {
                val notifications = (_notificationsResponse.value as Response.Success<List<Pair<IBoardNotification, IGroup>>>).value.toMutableList()
                notifications.remove(Pair(notification, group))
                _notificationsResponse.value = Response.Success(notifications)
            }
        }
    }

    fun resetPostResponse() {
        _postResponse.value = null
    }

    fun batchDelete(selectedItems: List<Pair<IGroup, IBoardNotification>>, apiContext: ApiContext) {
        viewModelScope.launch {
            val responses = selectedItems.map { boardRepository.deleteBoardNotification(it.second, it.first, apiContext) }
            _batchDeleteResponse.value = responses
            val notifications = notificationsResponse.value?.valueOrNull()
            if (notifications != null) {
                val currentNotifications = notifications.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentNotifications.remove(response.value)
                    }
                }
                _notificationsResponse.value = Response.Success(currentNotifications)
            }
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }

}