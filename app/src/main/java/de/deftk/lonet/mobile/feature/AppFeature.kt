package de.deftk.lonet.mobile.feature

import androidx.fragment.app.Fragment
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.mobile.AuthStore
import de.deftk.lonet.mobile.R
import de.deftk.lonet.mobile.feature.overview.*
import de.deftk.lonet.mobile.fragments.*

enum class AppFeature(
    val feature: Feature,
    val fragmentClass: Class<out Fragment>,
    val drawableResource: Int,
    val translationResource: Int,
    val overviewClass: Class<out AbstractOverviewElement>? = null,
    val overviewCreator: OverviewCreator? = null
) {

    FEATURE_TASKS(Feature.TASKS, TasksFragment::class.java, R.drawable.ic_tasks, R.string.tasks, TasksOverviewElement::class.java, object : OverviewCreator {
        override fun createOverview(overwriteCache: Boolean): AbstractOverviewElement {
            val tasks = AuthStore.appUser.getTasks(overwriteCache)
            return TasksOverviewElement(tasks.count { it.completed }, tasks.size)
        }
    }),
    FEATURE_MAIL(Feature.MAILBOX, MailFragment::class.java, R.drawable.ic_email, R.string.mail, MailOverview::class.java, object: OverviewCreator {
        override fun createOverview(overwriteCache: Boolean): AbstractOverviewElement {
            return MailOverview(AuthStore.appUser.getEmailQuota(overwriteCache), AuthStore.appUser.getUnreadEmailCount(overwriteCache))
        }
    }),
    FEATURE_FILE_STORAGE(Feature.FILES, FileStorageFragment::class.java, R.drawable.ic_files, R.string.file_storage, FileStorageOverview::class.java, object: OverviewCreator {
        override fun createOverview(overwriteCache: Boolean): AbstractOverviewElement {
            return FileStorageOverview(AuthStore.appUser.getFileQuota(AuthStore.appUser.sessionId, overwriteCache))
        }
    }),
    FEATURE_NOTIFICATIONS(Feature.BOARD, NotificationsFragment::class.java, R.drawable.ic_notifications, R.string.notifications, NotificationsOverview::class.java, object : OverviewCreator {
        override fun createOverview(overwriteCache: Boolean): AbstractOverviewElement {
            return NotificationsOverview(AuthStore.appUser.getNotifications(overwriteCache).size)
        }
    }),
    FEATURE_FORUM(Feature.FORUM, ForumFragment::class.java, R.drawable.ic_forum, R.string.forum),
    FEATURE_SYSTEM_NOTIFICATIONS(Feature.MESSAGES, SystemNotificationsFragment::class.java, R.drawable.ic_system_notifications, R.string.system_notifications, SystemNotificationsOverview::class.java, object : OverviewCreator {
        override fun createOverview(overwriteCache: Boolean): AbstractOverviewElement {
            return SystemNotificationsOverview(AuthStore.appUser.getSystemNofications(overwriteCache).count { !it.read })
        }
    });

    companion object {
        fun getByAPIFeature(feature: Feature): AppFeature? {
            return values().firstOrNull { it.feature == feature }
        }

        fun getByOverviewClass(overviewClass: Class<out AbstractOverviewElement>): AppFeature? {
            return values().firstOrNull { it.overviewClass == overviewClass }
        }
    }

}