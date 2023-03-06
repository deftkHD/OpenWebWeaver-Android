package de.deftk.openww.android.fragments.feature.board.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.fragments.feature.board.ReadNotificationFragmentDirections
import de.deftk.openww.android.repository.board.BoardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadNotificationViewModel @Inject constructor(
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

    val uiState: StateFlow<ReadNotificationFragmentUIState> = notification
        .map {
            if (it == null)
                ReadNotificationFragmentUIState.Loading
            else
                ReadNotificationFragmentUIState.Success(it)
        }
        .catch { ReadNotificationFragmentUIState.Failure(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadNotificationFragmentUIState.Loading)

    fun setNotification(notificationId: String, login: String) {
        this.notificationId.update {
            NotificationId(notificationId, login)
        }
    }

    suspend fun getNotification(): BoardNotification? {
        return notification.first()
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            boardRepository.refreshNotifications()
        }
    }

    fun navigateEditNotification(navController: NavController) {
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
                    boardRepository.deleteBoardNotification(notification)
                }
            }
        }
    }

    private data class NotificationId(val notificationId: String, val login: String)

}

sealed interface ReadNotificationFragmentUIState {
    object Loading : ReadNotificationFragmentUIState
    data class Failure(val throwable: Throwable) : ReadNotificationFragmentUIState
    data class Success(val notification: BoardNotification) : ReadNotificationFragmentUIState
}