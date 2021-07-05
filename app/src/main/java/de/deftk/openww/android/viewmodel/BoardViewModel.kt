package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import de.deftk.openww.api.model.feature.board.IBoardNotification
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.BoardRepository
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BoardViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val boardRepository: BoardRepository): ViewModel() {

    private val _notificationsResponse = MutableLiveData<Response<List<Pair<IBoardNotification, IGroup>>>>()
    val notificationsResponse: LiveData<Response<List<Pair<IBoardNotification, IGroup>>>> = _notificationsResponse

    private val _postResponse = MutableLiveData<Response<IBoardNotification?>?>()
    val postResponse: LiveData<Response<IBoardNotification?>?> = _postResponse

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
            if (_notificationsResponse.value is Response.Success && response is Response.Success) {
                // inject new notification into stored livedata
                val notifications = (_notificationsResponse.value as Response.Success<List<Pair<IBoardNotification, IGroup>>>).value.toMutableList()
                notifications.add(response.value to group)
                _notificationsResponse.value = Response.Success(notifications)
            }
            _postResponse.value = response
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
            if (response is Response.Success) {
                // no need to update items in list because the instance will be the same

                // trigger observers
                _notificationsResponse.value = _notificationsResponse.value
            }
            _postResponse.value = response.smartMap { notification }
        }
    }

    fun deleteBoardNotification(notification: IBoardNotification, group: IGroup, apiContext: ApiContext) {
        viewModelScope.launch {
            val response = boardRepository.deleteBoardNotification(notification, group, apiContext)
            if (response is Response.Success && _notificationsResponse.value is Response.Success) {
                val notifications = (_notificationsResponse.value as Response.Success<List<Pair<IBoardNotification, IGroup>>>).value.toMutableList()
                notifications.remove(Pair(notification, group))
                _notificationsResponse.value = Response.Success(notifications)
            }
            _postResponse.value = Response.Success(notification)
        }
    }

    fun resetPostResponse() {
        _postResponse.value = null
    }

}