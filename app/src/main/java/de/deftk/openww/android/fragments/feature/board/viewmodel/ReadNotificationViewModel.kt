package de.deftk.openww.android.fragments.feature.board.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.exception.ObjectNotFoundException
import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.fragments.feature.board.ReadNotificationFragmentDirections
import de.deftk.openww.android.repository.board.BoardRepository
import de.deftk.openww.api.model.Permission
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadNotificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository
) : ViewModel() {

    val notificationId = savedStateHandle.get<String>("notificationId")
    private val groupId = savedStateHandle.get<String>("groupId")

    private var deleted = false

    private val notification = boardRepository.notifications.map { notifications ->
        if (notifications?.isNotEmpty() == true && !deleted) {
            notifications.singleOrNull {
                it.notification.id == notificationId && it.group.login == groupId
            } ?: throw ObjectNotFoundException("Notification")
        } else {
            null
        }
    }

    val uiState: StateFlow<ReadNotificationFragmentUIState> = notification
        .map { notification ->
            if (notification == null) {
                if (deleted) {
                    ReadNotificationFragmentUIState.Closed
                } else {
                    ReadNotificationFragmentUIState.Loading
                }
            } else {
                ReadNotificationFragmentUIState.Success(notification)
            }
        }
        .catch {
            emit(ReadNotificationFragmentUIState.Failure(it))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadNotificationFragmentUIState.Loading)

    suspend fun canEdit(): Boolean {
        val notification = notification.first() ?: return false
        val group = notification.group
        return group.effectiveRights.contains(Permission.BOARD_WRITE) || group.effectiveRights.contains(Permission.BOARD_ADMIN)
    }

    fun editNotification(navController: NavController) {
        viewModelScope.launch {
            notification.collectLatest { notification ->
                if (notification != null) {
                    navController.navigate(ReadNotificationFragmentDirections.actionReadNotificationFragmentToEditNotificationFragment(notification.notification.id, notification.group.login))
                }
            }
        }
    }

    fun deleteNotification() {
        viewModelScope.launch {
            notification.collectLatest { notification ->
                if (notification != null) {
                    deleted = true
                    boardRepository.deleteBoardNotification(notification)
                }
            }
        }
    }

}

sealed interface ReadNotificationFragmentUIState {
    object Loading : ReadNotificationFragmentUIState
    object Closed : ReadNotificationFragmentUIState
    data class Failure(val throwable: Throwable) : ReadNotificationFragmentUIState
    data class Success(val notification: BoardNotification) : ReadNotificationFragmentUIState
}