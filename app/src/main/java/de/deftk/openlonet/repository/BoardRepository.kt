package de.deftk.openlonet.repository

import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.IGroup
import de.deftk.lonet.api.model.IRequestContext
import de.deftk.lonet.api.model.feature.board.BoardNotificationColor
import de.deftk.lonet.api.model.feature.board.BoardType
import de.deftk.lonet.api.model.feature.board.IBoardNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class BoardRepository @Inject constructor() : AbstractRepository() {

    suspend fun getBoardNotifications(apiContext: ApiContext) = apiCall {
        apiContext.getUser().getAllBoardNotifications(apiContext).sortedByDescending { it.first.created.date.time }
    }

    suspend fun addBoardNotification(title: String, text: String, color: BoardNotificationColor?, killDate: Date?, group: IGroup, apiContext: ApiContext) = apiCall {
        group.addBoardNotification(
            title,
            text,
            color,
            killDate,
            group.getRequestContext(apiContext)
        )
    }

    suspend fun editBoardNotification(notification: IBoardNotification, title: String? = null, text: String? = null, color: BoardNotificationColor? = null, killDate: Date? = null, boardType: BoardType = BoardType.ALL, context: IRequestContext) = apiCall {
        notification.edit(title, text, color, killDate, boardType, context)
    }

    suspend fun deleteBoardNotification(notification: IBoardNotification, group: IGroup, apiContext: ApiContext) = apiCall {
        notification.delete(BoardType.ALL, group.getRequestContext(apiContext))
    }

}