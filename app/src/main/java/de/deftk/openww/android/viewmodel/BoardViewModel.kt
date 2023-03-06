package de.deftk.openww.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.filter.BoardNotificationFilter
import de.deftk.openww.android.repository.board.BoardRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import de.deftk.openww.api.model.feature.board.IBoardNotification
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BoardViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val boardRepository: BoardRepository): ScopedViewModel(savedStateHandle) {

    private val _notificationsResponse = registerProperty<Response<List<Pair<IBoardNotification, IGroup>>>?>("notificationsResponse", true)
    val allNotificationsResponse: LiveData<Response<List<Pair<IBoardNotification, IGroup>>>?> = _notificationsResponse

    val filter = registerProperty("filter", true, BoardNotificationFilter())

    val filteredNotificationResponse: LiveData<Response<List<Pair<IBoardNotification, IGroup>>>?>
        get() = filter.switchMap { filter ->
            when (filter) {
                null -> allNotificationsResponse
                else -> allNotificationsResponse.switchMap { response ->
                    val filtered = registerProperty<Response<List<Pair<IBoardNotification, IGroup>>>?>("filtered", true)
                    //filtered.postValue(response?.smartMap { filter.apply(it) })
                    filtered
                }
            }
        }

    private val _postResponse = registerProperty<Response<IBoardNotification?>?>("postResponse", true)
    val postResponse: LiveData<Response<IBoardNotification?>?> = _postResponse

    private val _batchDeleteResponse = registerProperty<List<Response<Pair<IBoardNotification, IGroup>>>?>("batchDeleteResponse", true)
    val batchDeleteResponse: LiveData<List<Response<Pair<IBoardNotification, IGroup>>>?> = _batchDeleteResponse

    fun loadBoardNotifications(apiContext: IApiContext) {
        viewModelScope.launch {
            //_notificationsResponse.postValue(boardRepository.getBoardNotifications(apiContext))
        }
    }

    fun addBoardNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup, apiContext: IApiContext) {
        viewModelScope.launch {
            /*val response = boardRepository.addBoardNotification(
                title,
                text,
                color,
                killDate,
                group,
                apiContext
            )
            _postResponse.postValue(response)
            if (_notificationsResponse.value is Response.Success && response is Response.Success) {
                // inject new notification into stored livedata
                val notifications = (_notificationsResponse.value as Response.Success<List<Pair<IBoardNotification, IGroup>>>).value.toMutableList()
                notifications.add(response.value to group)
                _notificationsResponse.postValue(Response.Success(notifications))
            }*/
        }
    }

    fun editBoardNotification(notification: IBoardNotification, title: String, text: String, color: BoardNotificationColor, killDate: Date?, group: IGroup, apiContext: IApiContext) {
        viewModelScope.launch {
            /*val response = boardRepository.editBoardNotification(
                notification,
                title,
                text,
                color,
                killDate,
                BoardType.ALL,
                group.getRequestContext(apiContext)
            )
            _postResponse.postValue(response.smartMap { notification })
            if (response is Response.Success) {
                // no need to update items in list because the instance will be the same

                // trigger observers
                _notificationsResponse.postValue(_notificationsResponse.value)
            }*/
        }
    }

    fun deleteBoardNotification(notification: IBoardNotification, group: IGroup, apiContext: IApiContext) {
        viewModelScope.launch {
            /*val response = boardRepository.deleteBoardNotification(notification, group, apiContext)
            _postResponse.postValue(Response.Success(notification))
            if (response is Response.Success && _notificationsResponse.value is Response.Success) {
                val notifications = (_notificationsResponse.value as Response.Success<List<Pair<IBoardNotification, IGroup>>>).value.toMutableList()
                notifications.remove(Pair(notification, group))
                _notificationsResponse.postValue(Response.Success(notifications))
            }*/
        }
    }

    fun resetPostResponse() {
        _postResponse.postValue(null)
    }

    fun batchDelete(selectedItems: List<Pair<IGroup, IBoardNotification>>, apiContext: IApiContext) {
        viewModelScope.launch {
            /*val responses = selectedItems.map { boardRepository.deleteBoardNotification(it.second, it.first, apiContext) }
            _batchDeleteResponse.postValue(responses)
            val notifications = allNotificationsResponse.value?.valueOrNull()
            if (notifications != null) {
                val currentNotifications = notifications.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentNotifications.remove(response.value)
                    }
                }
                _notificationsResponse.postValue(Response.Success(currentNotifications))
            }*/
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.postValue(null)
    }

}