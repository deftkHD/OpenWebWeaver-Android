package de.deftk.lonet.mobile.feature

import androidx.fragment.app.Fragment
import com.google.gson.JsonObject
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.api.model.feature.SystemNotification
import de.deftk.lonet.api.model.feature.Task
import de.deftk.lonet.api.request.UserApiRequest
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
    val overviewBuilder: OverviewBuilder? = null
) {

    FEATURE_TASKS(Feature.TASKS, TasksFragment::class.java, R.drawable.ic_edit_24, R.string.tasks, TasksOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetAllTasksRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val tasks = mutableListOf<Task>()
            response.values.withIndex().forEach { (index, subResponse) ->
                if (index % 2 == 1) {
                    val focus = response.values.toList()[index - 1]
                    check(focus.get("method").asString == "set_focus")
                    val operator = AuthStore.appUser.getContext().getOperator(focus.get("user").asJsonObject.get("login").asString)!!
                    subResponse.get("entries").asJsonArray.forEach { taskResponse ->
                        tasks.add(Task.fromJson(taskResponse.asJsonObject, operator))
                    }
                }
            }
            return TasksOverview(tasks.count { it.completed }, tasks.size)
        }
    }),
    FEATURE_MAIL(Feature.MAILBOX, MailFragment::class.java, R.drawable.ic_email_24, R.string.mail, MailOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetEmailStateRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val quota = Quota.fromJson(subResponse.get("quota").asJsonObject)
            val unread = subResponse.get("unread_messages").asInt
            return MailOverview(quota, unread)
        }
    }),
    FEATURE_FILE_STORAGE(Feature.FILES, FileStorageFragment::class.java, R.drawable.ic_file_24, R.string.file_storage, FileStorageOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetFileStorageStateRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val quota = Quota.fromJson(subResponse.get("quota").asJsonObject)
            return FileStorageOverview(quota)
        }
    }),
    FEATURE_NOTIFICATIONS(Feature.BOARD, NotificationsFragment::class.java, R.drawable.ic_notifications_24, R.string.notifications, NotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetAllNotificationsRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            var count = 0
            response.values.withIndex().forEach { (index, subResponse) ->
                if (index % 2 == 1) {
                    val focus = response.values.toList()[index - 1]
                    check(focus.get("method").asString == "set_focus")
                    count += subResponse.get("entries").asJsonArray.size()
                }
            }
            return NotificationsOverview(count)
        }
    }),
    FEATURE_FORUM(Feature.FORUM, ForumFragment::class.java, R.drawable.ic_forum_24, R.string.forum),
    FEATURE_MEMBERS(Feature.MEMBERS, MembersFragment::class.java, R.drawable.ic_people_24, R.string.members),
    FEATURE_SYSTEM_NOTIFICATIONS(Feature.MESSAGES, SystemNotificationsFragment::class.java, R.drawable.ic_warning_24, R.string.system_notifications, SystemNotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetSystemNotificationsRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val systemNotifications = mutableListOf<SystemNotification>()
            subResponse.get("messages").asJsonArray.forEach { messageResponse ->
                systemNotifications.add(SystemNotification.fromJson(messageResponse.asJsonObject, AuthStore.appUser))
            }
            return SystemNotificationsOverview(systemNotifications.count { !it.read })
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