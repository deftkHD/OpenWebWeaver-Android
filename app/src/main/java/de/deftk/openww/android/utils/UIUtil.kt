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
        SystemNotificationType.FOLDER_FILE_UPLOAD to R.string.system_notification_folder_file_upload,
        SystemNotificationType.NEW_FORUM_COMMENT to R.string.system_notification_new_forum_comment,
        SystemNotificationType.NEW_COURSE to R.string.system_notification_new_course,
        // unknown 4
        SystemNotificationType.PASSWORD_CHANGED to R.string.system_notification_password_changed,
        SystemNotificationType.NEW_FORUM_POST to R.string.system_notification_new_forum_post,
        SystemNotificationType.FILE_UPLOAD to R.string.system_notification_file_upload,
        SystemNotificationType.FILE_DOWNLOAD to R.string.system_notification_file_download,
        // unknown 9
        SystemNotificationType.ADDED_TO_MESSENGER to R.string.system_notification_added_to_messenger,
        // unknown 11
        SystemNotificationType.CALENDAR_REMINDER to R.string.system_notification_calendar_reminder,
        SystemNotificationType.NEW_MAIL to R.string.new_mail,
        SystemNotificationType.NEW_GUESTBOOK_ENTRY to R.string.system_notification_new_guestbook_entry,
        SystemNotificationType.REQUEST_PASSWORD_RESET_CODE to R.string.system_notification_request_password_reset_code,
        // unknown 16
        // unknown 17
        // unknown 18
        SystemNotificationType.NEW_POLL to R.string.system_notification_new_poll,
        // unknown 20
        SystemNotificationType.RESOURCE_FAULTY to R.string.system_notification_resource_faulty,
        SystemNotificationType.RESOURCE_REPAIRED to R.string.system_notification_resource_repaired,
        SystemNotificationType.NEW_BLOG_COMMENT to R.string.system_notification_new_blog_comment,
        SystemNotificationType.NEW_BLOG_ENTRY to R.string.system_notification_new_blog_entry,
        SystemNotificationType.NEW_LEARNING_LOG_ENTRY to R.string.system_notification_new_learning_log_entry,
        SystemNotificationType.NEW_LEARNING_LOG_ENTRY_COMMENT to R.string.system_notification_new_learning_log_entry_comment,
        SystemNotificationType.NEW_LEARNING_LOG_COMMENT to R.string.system_notification_new_learning_log_comment,
        SystemNotificationType.NEW_LEARNING_LOG to R.string.system_notification_new_learning_log,
        SystemNotificationType.NEW_NOTIFICATION to R.string.system_notification_new_notification,
        SystemNotificationType.NEW_APPOINTMENT to R.string.system_notification_new_appointment,
        SystemNotificationType.NEW_POLL2 to R.string.system_notification_new_poll,
        // unknown 32
        SystemNotificationType.NEW_TRUST to R.string.system_notification_new_trust,
        // unknown 34
        SystemNotificationType.UNAUTHORIZED_LOGIN_LOCATION to R.string.system_notification_unauthorized_login_location,
        SystemNotificationType.NEW_WALL_ENTRY to R.string.system_notification_new_wall_entry,
        SystemNotificationType.NEW_WALL_COMMENT to R.string.system_notification_new_wall_comment,
        SystemNotificationType.NEW_SUBSTITUTION_PLAN to R.string.system_notification_new_subsittution_plan,
        SystemNotificationType.NEW_TEACHER_NOTIFICATION to R.string.system_notification_new_teacher_notification,
        SystemNotificationType.NEW_STUDENT_NOTIFICATION to R.string.system_notification_new_student_notification,
        SystemNotificationType.PENDING_QUICK_MESSAGE to R.string.system_notification_pending_quick_message,
        SystemNotificationType.COURSELET_CORRECTED to R.string.system_notification_courselet_corrected,
        SystemNotificationType.NEW_BOOKMARK to R.string.system_notification_new_bookmark,
        // unknown 44
        SystemNotificationType.RESOURCE_BOOKED to R.string.system_notification_resource_booked,
        SystemNotificationType.NEW_TASK to R.string.system_notification_new_task,
        SystemNotificationType.NEW_FORM_SUBMISSION to R.string.system_notification_new_form_submission,
        // unknown 48
        SystemNotificationType.NEW_CONSULTATION_HOUR_BOOKED to R.string.system_notification_new_consultation_hour_booked,
        SystemNotificationType.NEW_COURSELET to R.string.system_notification_new_courselet,
        SystemNotificationType.NEW_LEARNING_PLAN to R.string.system_notification_new_learning_plan
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