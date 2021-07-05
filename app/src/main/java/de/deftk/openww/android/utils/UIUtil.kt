package de.deftk.openww.android.utils

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.board.BoardNotificationColors
import de.deftk.openww.android.feature.notes.NoteColors
import de.deftk.openww.api.model.feature.board.IBoardNotification
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.api.model.feature.systemnotification.SystemNotificationType
import de.deftk.openww.api.model.feature.tasks.ITask
import java.text.DateFormat

object UIUtil {

    private val systemNotificationTranslationMap = mapOf(
        SystemNotificationType.PASSWORD_CHANGED to R.string.system_notification_password_changed,
        SystemNotificationType.FILE_UPLOAD to R.string.system_notification_file_upload,
        SystemNotificationType.FILE_DOWNLOAD to R.string.system_notification_file_download,
        SystemNotificationType.ADDED_TO_MESSENGER to R.string.system_notification_added_to_messenger,
        SystemNotificationType.REQUEST_PASSWORD_RESET_CODE to R.string.system_notification_request_password_reset_code,
        SystemNotificationType.NEW_POLL to R.string.system_notification_new_poll,
        SystemNotificationType.NEW_NOTIFICATION to R.string.system_notification_new_notification,
        SystemNotificationType.NEW_APPOINTMENT to R.string.system_notification_new_appointment,
        SystemNotificationType.NEW_TRUST to R.string.system_notification_new_trust,
        SystemNotificationType.UNAUTHORIZED_LOGIN_LOCATION to R.string.system_notification_unauthorized_login_location,
        SystemNotificationType.NEW_TASK to R.string.system_notification_new_task
    )

    @StringRes
    fun getTranslatedSystemNotificationTitle(systemNotification: ISystemNotification): Int {
        return systemNotificationTranslationMap[systemNotification.messageType] ?: R.string.system_notification_type_unknown
    }

    @ColorRes
    fun getBoardNotificationAccent(boardNotification: IBoardNotification): Int {
        return BoardNotificationColors.getByApiColorOrDefault(boardNotification.color).androidColor
    }

    @ColorRes
    fun getNoteAccent(note: INote): Int {
        return NoteColors.getByApiColorOrDefault(note.color).androidColor
    }

    fun getTaskDue(task: ITask): String {
        val date = task.dueDate
        if (date != null)
            return DateFormat.getDateInstance().format(date)
        return ""
    }

}