package de.deftk.openww.android.repository

import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IRequestContext
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.api.model.feature.board.BoardType
import de.deftk.openww.api.model.feature.board.IBoardNotification
import java.util.*
import javax.inject.Inject

class BoardRepository @Inject constructor() : AbstractRepository() {

    suspend fun getBoardNotifications(apiContext: IApiContext) = apiCall {
        apiContext.user.getAllBoardNotifications(apiContext)
    }

    suspend fun addBoardNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup, apiContext: IApiContext) = apiCall {
        group.addBoardNotification(
            title,
            text,
            color,
            killDate,
            group.getRequestContext(apiContext)
        )
    }

    suspend fun editBoardNotification(notification: IBoardNotification, title: String, text: String, color: BoardNotificationColor, killDate: Date? = null, boardType: BoardType = BoardType.ALL, context: IRequestContext) = apiCall {
        notification.edit(title, text, color, killDate, boardType, context)
    }

    suspend fun deleteBoardNotification(notification: IBoardNotification, group: IGroup, apiContext: IApiContext) = apiCall {
        notification.delete(BoardType.ALL, group.getRequestContext(apiContext))
        notification to group
    }

}