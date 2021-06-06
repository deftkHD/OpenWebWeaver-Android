package de.deftk.openlonet.utils

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.deftk.lonet.api.model.feature.board.IBoardNotification
import de.deftk.lonet.api.model.feature.systemnotification.ISystemNotification
import de.deftk.lonet.api.model.feature.systemnotification.SystemNotificationType
import de.deftk.lonet.api.model.feature.tasks.ITask
import de.deftk.openlonet.R
import de.deftk.openlonet.feature.board.BoardNotificationColors
import java.text.DateFormat

object UIUtil {

    private val systemNotificationTranslationMap = mapOf(
        Pair(SystemNotificationType.FILE_DOWNLOAD, R.string.system_notification_type_file_download),
        Pair(SystemNotificationType.FILE_UPLOAD, R.string.system_notification_type_file_upload),
        Pair(SystemNotificationType.NEW_NOTIFICATION, R.string.system_notification_type_new_notification),
        Pair(SystemNotificationType.NEW_TRUST, R.string.system_notification_type_new_trust),
        Pair(SystemNotificationType.UNAUTHORIZED_LOGIN_LOCATION, R.string.system_notification_unauthorized_login_location),
        Pair(SystemNotificationType.NEW_TASK, R.string.system_notification_type_new_task)
    )

    @StringRes
    fun getTranslatedSystemNotificationTitle(systemNotification: ISystemNotification): Int {
        return systemNotificationTranslationMap[systemNotification.messageType] ?: R.string.system_notification_type_unknown
    }

    @ColorRes
    fun getBoardNotificationAccent(boardNotification: IBoardNotification): Int {
        return BoardNotificationColors.getByApiColorOrDefault(boardNotification.getColor()).androidColor
    }

    fun getTaskDue(task: ITask): String {
        val date = task.getEndDate()
        if (date != null)
            return DateFormat.getDateInstance().format(date)
        return ""
    }

}