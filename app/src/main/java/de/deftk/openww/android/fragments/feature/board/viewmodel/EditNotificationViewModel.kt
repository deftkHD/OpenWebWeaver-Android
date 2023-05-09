package de.deftk.openww.android.fragments.feature.board.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.exception.ObjectNotFoundException
import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.repository.board.BoardRepository
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditNotificationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository
) : ViewModel() {

    val notificationId = savedStateHandle.get<String>("notificationId")

    var groupId = savedStateHandle.get<String>("groupId")
        set(value) {
            savedStateHandle["groupId"] = value
            field = value
        }

    var title: String = savedStateHandle.get<String>("title") ?: ""
        set(value) {
            savedStateHandle["title"] = value
            field = value
        }

    var text: String = savedStateHandle.get<String>("text") ?: ""
        set(value) {
            savedStateHandle["text"] = value
            field = value
        }

    var color: Int = savedStateHandle.get<Int>("color") ?: 0
        set(value) {
            savedStateHandle["color"] = value
            field = value
        }

    private val notification = boardRepository.notifications.map { notifications ->
        if (notifications?.isNotEmpty() == true && notificationId != null && groupId != null) {
            notifications.singleOrNull {
                it.notification.id == notificationId && it.group.login == groupId
            } ?: throw ObjectNotFoundException("Notification")
        } else {
            null
        }
    }

    val uiState: StateFlow<EditNotificationFragmentUIState> = notification
        .map { notification ->
            if (notificationId != null && notification == null) {
                EditNotificationFragmentUIState.Loading
            } else {
                val apiContext = boardRepository.getApiContext()
                val groups = apiContext?.user?.getGroups()?.filter { it.effectiveRights.contains(Permission.BOARD_WRITE) || it.effectiveRights.contains(Permission.BOARD_ADMIN) } ?: emptyList()
                title = notification?.notification?.title ?: ""
                text = notification?.notification?.text ?: ""
                color = notification?.notification?.color?.serialId ?: 0
                EditNotificationFragmentUIState.Success(notification, groups)
            }
        }
        .catch { emit(EditNotificationFragmentUIState.Failure(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EditNotificationFragmentUIState.Loading)

    private val eventChannel = Channel<EditNotificationEvent>()
    val eventChannelFlow = eventChannel.receiveAsFlow()

    fun submitAction(title: String, text: String, color: BoardNotificationColor, killDate: Date?, group: IGroup?) {
        //TODO text not allowed to be empty
        viewModelScope.launch {
            if (notificationId != null && groupId != null) {
                notification.first()?.also { notification ->
                    boardRepository.editBoardNotification(notification, title, text, color, killDate, BoardType.ALL)
                    eventChannel.send(EditNotificationEvent.Edited)
                }
            } else if (group != null) {
                boardRepository.addBoardNotification(title, text, color, killDate, group)
                eventChannel.send(EditNotificationEvent.Added)
            } else {
                eventChannel.send(EditNotificationEvent.InvalidGroup)
            }
        }
    }

    sealed interface EditNotificationEvent {
        object Added : EditNotificationEvent
        object Edited : EditNotificationEvent
        object InvalidGroup : EditNotificationEvent
    }

}

sealed interface EditNotificationFragmentUIState {
    object Loading : EditNotificationFragmentUIState
    data class Failure(val throwable: Throwable) : EditNotificationFragmentUIState
    data class Success(val notification: BoardNotification?, val effectiveGroups: List<IGroup>) : EditNotificationFragmentUIState
}