package de.deftk.openww.android.repository.board

import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.repository.login.LoginRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

class BoardRepository(private val loginRepository: LoginRepository) {

    private val remoteDataSource = BoardRemoteDataSource()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _notifications = loginRepository.apiContext.flatMapLatest { apiContext ->
        remoteDataSource.checkNeedsUpdate(apiContext)
        remoteDataSource.notifications
    }

    val notifications: Flow<List<BoardNotification>>
        get() = _notifications

    suspend fun getApiContext(): IApiContext? {
        return loginRepository.apiContext.first()
    }

    suspend fun refreshNotifications() {
        loginRepository.apiContext.collectLatest { apiContext ->
            remoteDataSource.refreshBoardNotifications(apiContext)
        }
    }

    suspend fun addBoardNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup) {
        loginRepository.apiContext.collectLatest { apiContext ->
            if (apiContext != null) {
                remoteDataSource.addBoardNotification(title, text, color, killDate, group, apiContext)
            }
        }
    }

    suspend fun editBoardNotification(notification: BoardNotification, title: String, text: String, color: BoardNotificationColor, killDate: Date? = null, boardType: BoardType = BoardType.ALL) {
        loginRepository.apiContext.collectLatest { apiContext ->
            if (apiContext != null) {
                remoteDataSource.editBoardNotification(notification, title, text, color, killDate, boardType, apiContext)
            }
        }
    }

    suspend fun deleteBoardNotification(notification: BoardNotification) {
        loginRepository.apiContext.collectLatest { apiContext ->
            if (apiContext != null) {
                remoteDataSource.deleteBoardNotification(notification, apiContext)
            }
        }
    }

    suspend fun batchDeleteBoardNotifications(notifications: List<BoardNotification>) {
        loginRepository.apiContext.collectLatest { apiContext ->
            if (apiContext != null) {
                remoteDataSource.batchDeleteBoardNotifications(notifications, apiContext)
            }
        }
    }

}