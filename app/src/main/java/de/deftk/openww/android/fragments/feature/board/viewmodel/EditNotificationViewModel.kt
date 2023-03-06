package de.deftk.openww.android.fragments.feature.board.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.repository.board.BoardRepository
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditNotificationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val notificationId = MutableStateFlow<NotificationId?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val notification = notificationId.flatMapLatest { notificationId ->
        boardRepository.notifications.map { notifications ->
            if (notificationId == null) {
                null
            } else {
                notifications.singleOrNull {
                    it.notification.id == notificationId.notificationId && it.group.login == notificationId.login
                }
            }
        }
    }

    val uiState: StateFlow<EditNotificationFragmentUIState> = notification
        .map {
            val apiContext = boardRepository.getApiContext()
            val groups = apiContext?.user?.getGroups()?.filter { it.effectiveRights.contains(Permission.BOARD_WRITE) || it.effectiveRights.contains(Permission.BOARD_ADMIN) } ?: emptyList()
            EditNotificationFragmentUIState.Success(it, groups)
        }
        .catch { EditNotificationFragmentUIState.Failure(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EditNotificationFragmentUIState.Loading)

    fun setNotification(notificationId: String, login: String) {
        this.notificationId.update {
            NotificationId(notificationId, login)
        }
    }

    fun addNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup) {
        viewModelScope.launch {
            boardRepository.addBoardNotification(title, text, color, killDate, group)
        }
    }

    fun editNotification(title: String, text: String, color: BoardNotificationColor, killDate: Date?) {
        viewModelScope.launch {
            notification.collectLatest { notification ->
                if (notification != null) {
                    boardRepository.editBoardNotification(notification, title, text, color, killDate, BoardType.ALL)
                }
            }
        }
    }

    fun deleteNotification() {
        viewModelScope.launch {
            notification.collectLatest { notification ->
                if (notification != null) {
                    boardRepository.deleteBoardNotification(notification)
                }
            }
        }
    }

    private data class NotificationId(val notificationId: String, val login: String)

}

sealed interface EditNotificationFragmentUIState {
    object Loading : EditNotificationFragmentUIState
    data class Failure(val throwable: Throwable) : EditNotificationFragmentUIState
    data class Success(val notification: BoardNotification?, val effectiveGroups: List<IGroup>) : EditNotificationFragmentUIState
}