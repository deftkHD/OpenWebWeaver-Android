package de.deftk.openww.android.repository.board

import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.repository.RemoteDataSource
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class BoardRemoteDataSource: RemoteDataSource() {

    private val _notifications = MutableStateFlow(listOf<BoardNotification>())
    val notifications: Flow<List<BoardNotification>>
        get() = _notifications

    private var contextOfData: Int? = null

    suspend fun checkNeedsUpdate(apiContext: IApiContext?) {
        val currentContext = apiContext?.hashCode()
        if (contextOfData != currentContext) {
            refreshBoardNotifications(apiContext)
            contextOfData = currentContext
        }
    }

    suspend fun refreshBoardNotifications(apiContext: IApiContext?) {
        _notifications.update {
            if (apiContext != null) {
                //TODO check permissions
                withContext(Dispatchers.IO) {
                    apiContext.user.getAllBoardNotifications(apiContext).map { BoardNotification(it.first, it.second) }
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun addBoardNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup, apiContext: IApiContext) {
        //TODO check permissions
        val added = withContext(Dispatchers.IO) {
            group.addBoardNotification(title, text, color, killDate, group.getRequestContext(apiContext))
        }
        val notification = BoardNotification(added, group)

        _notifications.update { source ->
            val list = source.toMutableList()
            list.add(notification)
            list.toList()
        }
    }

    suspend fun editBoardNotification(notification: BoardNotification, title: String, text: String, color: BoardNotificationColor, killDate: Date? = null, boardType: BoardType = BoardType.ALL, apiContext: IApiContext) {
        //TODO check permissions
        withContext(Dispatchers.IO) {
            notification.notification.edit(title, text, color, killDate, boardType, notification.group.getRequestContext(apiContext))
        }

        _notifications.update { source ->
            val list = source.toMutableList()
            //FIXME this is not working
            object : ArrayList<BoardNotification>(list) {
                override fun equals(other: Any?): Boolean {
                    return false
                }
            }
        }
    }

    suspend fun deleteBoardNotification(notification: BoardNotification, apiContext: IApiContext) {
        //TODO check permissions
        withContext(Dispatchers.IO) {
            notification.notification.delete(BoardType.ALL, notification.group.getRequestContext(apiContext))
        }
        _notifications.update { source ->
            val list = source.toMutableList()
            list.removeAll { it.group.login == notification.group.login && it.notification.id == notification.notification.id }
            list.toList()
        }
    }

    suspend fun batchDeleteBoardNotifications(notifications: List<BoardNotification>, apiContext: IApiContext) {
        //TODO bundle into one request
        notifications.forEach { notification ->
            deleteBoardNotification(notification, apiContext)
        }
    }


}