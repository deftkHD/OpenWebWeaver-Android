package de.deftk.openww.android.repository.board

import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.repository.login.LoginRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import kotlinx.coroutines.flow.*
import java.util.*

class BoardRepository(private val loginRepository: LoginRepository) {

    private val remoteDataSource = BoardRemoteDataSource()

    private val _notifications = combine(
        loginRepository.apiContext,
        remoteDataSource.notifications
    ) { apiContext, notifications ->
        remoteDataSource.checkNeedsUpdate(apiContext)
        notifications
    }

    val notifications: Flow<List<BoardNotification>?>
        get() = _notifications

    suspend fun getApiContext(): IApiContext? {
        return loginRepository.apiContext.first()
    }

    suspend fun refreshNotifications() {
        loginRepository.apiContext.first()?.also { apiContext ->
            remoteDataSource.refreshBoardNotifications(apiContext)
        }
    }

    suspend fun addBoardNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup) {
        loginRepository.apiContext.first()?.also { apiContext ->
            remoteDataSource.addBoardNotification(title, text, color, killDate, group, apiContext)
        }
    }

    suspend fun editBoardNotification(notification: BoardNotification, title: String, text: String, color: BoardNotificationColor, killDate: Date? = null, boardType: BoardType = BoardType.ALL) {
        loginRepository.apiContext.first()?.also { apiContext ->
            remoteDataSource.editBoardNotification(notification, title, text, color, killDate, boardType, apiContext)
        }
    }

    suspend fun deleteBoardNotification(notification: BoardNotification) {
        loginRepository.apiContext.first()?.also { apiContext ->
            remoteDataSource.deleteBoardNotification(notification, apiContext)
        }
    }

    suspend fun batchDeleteBoardNotifications(notifications: List<BoardNotification>) {
        loginRepository.apiContext.first()?.also { apiContext ->
            remoteDataSource.batchDeleteBoardNotifications(notifications, apiContext)
        }
    }

}