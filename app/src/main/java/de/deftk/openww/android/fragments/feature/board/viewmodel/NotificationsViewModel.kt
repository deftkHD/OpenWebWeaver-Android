package de.deftk.openww.android.fragments.feature.board.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.filter.BoardNotificationFilter
import de.deftk.openww.android.repository.board.BoardRepository
import de.deftk.openww.api.model.Permission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository
) : ViewModel() {

    //TODO restore filter from savedStateHandle (via parcelize)

    private val filter = MutableStateFlow(BoardNotificationFilter())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredNotifications = filter.flatMapLatest { filter ->
        boardRepository.notifications.map { items ->
            if (items == null)
                null
            else
                filter.apply(items)
        }
    }

    val uiState: StateFlow<NotificationsFragmentUIState> = filteredNotifications
        .map { notifications ->
            if (notifications == null) {
                NotificationsFragmentUIState.Loading
            } else {
                val apiContext = boardRepository.getApiContext()
                val canAdd = apiContext?.user?.getGroups()?.any { it.effectiveRights.contains(Permission.BOARD_WRITE) || it.effectiveRights.contains(Permission.BOARD_ADMIN) } == true
                NotificationsFragmentUIState.Success(notifications, canAdd)
            }
        }
        .catch { emit(NotificationsFragmentUIState.Failure(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsFragmentUIState.Loading)

    fun refreshNotifications() {
        viewModelScope.launch {
            boardRepository.refreshNotifications()
        }
    }

    fun deleteNotification(notification: BoardNotification) {
        viewModelScope.launch {
            boardRepository.deleteBoardNotification(notification)
        }
    }

    fun batchDeleteNotifications(notifications: List<BoardNotification>) {
        viewModelScope.launch {
            boardRepository.batchDeleteBoardNotifications(notifications)
        }
    }

    fun setFilter(itemFilter: BoardNotificationFilter) {
        filter.update { itemFilter }
    }

    fun getFilter(): BoardNotificationFilter {
        return filter.value
    }

}

sealed interface NotificationsFragmentUIState {
    object Loading : NotificationsFragmentUIState
    data class Failure(val throwable: Throwable): NotificationsFragmentUIState
    data class Success(val data: List<BoardNotification>, val canAddNotification: Boolean): NotificationsFragmentUIState
}