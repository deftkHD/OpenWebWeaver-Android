package de.deftk.openlonet.feature

import androidx.fragment.app.Fragment
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.implementation.feature.systemnotification.SystemNotification
import de.deftk.lonet.api.implementation.feature.tasks.Task
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.api.request.UserApiRequest
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.feature.overview.*
import de.deftk.openlonet.fragments.*
import kotlinx.serialization.json.*

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
            return request.addGetAllTasksRequest(AuthStore.getApiUser())
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val tasks = mutableListOf<Task>()
            response.values.withIndex().forEach { (index, subResponse) ->
                if (index % 2 == 1) {
                    val focus = response.values.toList()[index - 1]
                    check(focus["method"]!!.jsonPrimitive.content == "set_focus")
                    subResponse["entries"]!!.jsonArray.forEach { taskResponse ->
                        tasks.add(Json.decodeFromJsonElement(taskResponse))
                    }
                }
            }
            return TasksOverview(tasks.count { it.isCompleted() }, tasks.size)
        }
    }),
    FEATURE_MAIL(Feature.MAILBOX, MailFragment::class.java, R.drawable.ic_email_24, R.string.mail, MailOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetEmailStateRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val quota = LoNetClient.json.decodeFromJsonElement<Quota>(subResponse["quota"]!!)
            val unread = subResponse["unread_messages"]!!.jsonPrimitive.int
            return MailOverview(quota, unread)
        }
    }),
    FEATURE_FILE_STORAGE(Feature.FILES, FileStorageGroupFragment::class.java, R.drawable.ic_file_24, R.string.file_storage, FileStorageOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetFileStorageStateRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val quota = LoNetClient.json.decodeFromJsonElement<Quota>(subResponse["quota"]!!)
            return FileStorageOverview(quota)
        }
    }),
    FEATURE_NOTIFICATIONS(Feature.BOARD, NotificationsFragment::class.java, R.drawable.ic_notifications_24, R.string.notifications, NotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetAllBoardNotificationsRequest(AuthStore.getApiUser())
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            var count = 0
            response.values.withIndex().forEach { (index, subResponse) ->
                if (index % 2 == 1) {
                    val focus = response.values.toList()[index - 1]
                    check(focus["method"]!!.jsonPrimitive.content == "set_focus")
                    count += subResponse["entries"]!!.jsonArray.size
                }
            }
            return NotificationsOverview(count)
        }
    }),
    FEATURE_FORUM(Feature.FORUM, ForumGroupFragment::class.java, R.drawable.ic_forum_24, R.string.forum),
    FEATURE_MEMBERS(Feature.MEMBERS, MembersGroupFragment::class.java, R.drawable.ic_people_24, R.string.members),
    FEATURE_SYSTEM_NOTIFICATIONS(Feature.MESSAGES, SystemNotificationsFragment::class.java, R.drawable.ic_warning_24, R.string.system_notifications, SystemNotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest): List<Int> {
            return request.addGetSystemNotificationsRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val systemNotifications = mutableListOf<SystemNotification>()
            subResponse["messages"]!!.jsonArray.forEach { messageResponse ->
                systemNotifications.add(Json.decodeFromJsonElement(messageResponse))
            }
            return SystemNotificationsOverview(systemNotifications.count { it.isUnread })
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