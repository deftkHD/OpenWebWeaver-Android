package de.deftk.openww.android.feature

import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.implementation.feature.systemnotification.SystemNotification
import de.deftk.openww.api.implementation.feature.tasks.Task
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.request.UserApiRequest
import de.deftk.openww.android.R
import de.deftk.openww.android.feature.overview.*
import kotlinx.serialization.json.*

enum class AppFeature(
    val feature: Feature,
    val fragmentId: Int,
    val overviewClass: Class<out AbstractOverviewElement>? = null,
    val overviewBuilder: OverviewBuilder? = null
) {

    FEATURE_TASKS(Feature.TASKS, R.id.tasksFragment, TasksOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            return request.addGetAllTasksRequest(user)
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
    FEATURE_MAIL(Feature.MAILBOX, R.id.mailFragment, MailOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            return request.addGetEmailStateRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val quota = WebWeaverClient.json.decodeFromJsonElement<Quota>(subResponse["quota"]!!)
            val unread = subResponse["unread_messages"]!!.jsonPrimitive.int
            return MailOverview(quota, unread)
        }
    }),
    FEATURE_FILE_STORAGE(Feature.FILES, R.id.fileStorageGroupFragment, FileStorageOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            return request.addGetFileStorageStateRequest()
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>): AbstractOverviewElement {
            val subResponse = response.values.toList()[1]
            val quota = WebWeaverClient.json.decodeFromJsonElement<Quota>(subResponse["quota"]!!)
            return FileStorageOverview(quota)
        }
    }),
    FEATURE_NOTIFICATIONS(Feature.BOARD, R.id.notificationsFragment, NotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            return request.addGetAllBoardNotificationsRequest(user)
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
    FEATURE_FORUM(Feature.FORUM, R.id.forumGroupFragment),
    FEATURE_MEMBERS(Feature.MEMBERS, R.id.membersGroupFragment),
    FEATURE_SYSTEM_NOTIFICATIONS(Feature.MESSAGES, R.id.systemNotificationsFragment, SystemNotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
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
    }),
    FEATURE_MESSENGER(Feature.MESSENGER, R.id.chatsFragment),
    FEATURE_CONTACTS(Feature.ADDRESSES, R.id.contactsGroupFragment);

    companion object {
        fun getByOverviewClass(overviewClass: Class<out AbstractOverviewElement>): AppFeature? {
            return values().firstOrNull { it.overviewClass == overviewClass }
        }
    }

}