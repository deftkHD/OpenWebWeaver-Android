package de.deftk.openww.android.utils

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.board.BoardNotificationColors
import de.deftk.openww.android.feature.notes.NoteColors
import de.deftk.openww.api.model.feature.board.IBoardNotification
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.systemnotification.INotificationSetting
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.api.model.feature.systemnotification.NotificationFacility
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
        SystemNotificationType.NEW_POLL2 to R.string.system_notification_new_poll,
        SystemNotificationType.NEW_NOTIFICATION to R.string.system_notification_new_notification,
        SystemNotificationType.NEW_APPOINTMENT to R.string.system_notification_new_appointment,
        SystemNotificationType.NEW_TRUST to R.string.system_notification_new_trust,
        SystemNotificationType.UNAUTHORIZED_LOGIN_LOCATION to R.string.system_notification_unauthorized_login_location,
        SystemNotificationType.NEW_TASK to R.string.system_notification_new_task
    )

    @StringRes
    @Deprecated("Don't hardcode translations; request them from wwschool instead per system notification settings")
    fun getTranslatedSystemNotificationTitle(systemNotification: ISystemNotification): Int {
        return getTranslatedSystemNotificationTitle(systemNotification.messageType)
    }

    @StringRes
    @Deprecated("Don't hardcode translations; request them from wwschool instead per system notification settings")
    fun getTranslatedSystemNotificationTitle(systemNotificationType: SystemNotificationType): Int {
        return systemNotificationTranslationMap[systemNotificationType] ?: R.string.system_notification_type_unknown
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

    @StringRes
    fun translateNotificationFacility(facility: NotificationFacility): Int {
        return when (facility) {
            NotificationFacility.NORMAL -> R.string.normal
            NotificationFacility.QUICK_MESSAGE -> R.string.quick_message
            NotificationFacility.MAIL -> R.string.mail
            NotificationFacility.PUSH_NOTIFICATION -> R.string.push_notification
            NotificationFacility.DIGEST -> R.string.digest
            NotificationFacility.DIGEST_WEEKLY -> R.string.digest_weekly
            NotificationFacility.SMS -> R.string.sms
            else -> R.string.unknown
        }
    }

    @StringRes
    fun translateNotificationSettingObj(setting: INotificationSetting): Int {
        return when (setting.obj) {
            "wall" -> R.string.wall
            "mail" -> R.string.mail
            "bookmarks" -> R.string.bookmarks
            "board" -> R.string.notifications
            "board_teacher" -> R.string.notifications_teacher
            "board_pupil" -> R.string.notifications_pupil
            "messenger" -> R.string.messenger
            "substitution_plan" -> R.string.substitution_plan
            "calendar" -> R.string.calendar
            "tasks" -> R.string.tasks
            "learning_plan" -> R.string.learning_plan
            "courselets" -> R.string.courselets
            "forum" -> R.string.forum
            "files" -> R.string.file_storage
            "ws_gen" -> R.string.ws_gen
            "blog" -> R.string.blog
            "poll" -> R.string.poll
            "forms" -> R.string.forms
            "learning_log" -> R.string.learning_log
            "courses" -> R.string.courses
            "resource_management" -> R.string.resource_management
            "consultation_hours" -> R.string.consultation_hours
            "trusts" -> R.string.trusts
            else -> {
                println(setting.obj)
                R.string.unknown
            }
        }
    }

}