package de.deftk.openww.android.feature

import de.deftk.openww.android.R
import de.deftk.openww.android.feature.overview.*
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.implementation.feature.systemnotification.SystemNotification
import de.deftk.openww.api.implementation.feature.tasks.Task
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.request.UserApiRequest
import kotlinx.serialization.json.*

enum class AppFeature(
    val feature: Feature,
    val fragmentId: Int,
    val overviewClass: Class<out AbstractOverviewElement>? = null,
    val overviewBuilder: OverviewBuilder? = null,
    val preferenceName: String? = null
) {

    FEATURE_TASKS(Feature.TASKS, R.id.tasksFragment, TasksOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            return request.addGetAllTasksRequest(user)
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>, apiContext: IApiContext): AbstractOverviewElement {
            val tasks = mutableListOf<Task>()
            response.values.forEach { subResponse ->
                subResponse["entries"]!!.jsonArray.forEach { taskResponse ->
                    tasks.add(WebWeaverClient.json.decodeFromJsonElement(taskResponse))
                }
            }
            return TasksOverview(tasks.count { it.completed }, tasks.size) //FIXME ignore ignored
        }
    }, "overview_show_tasks"),
    FEATURE_MAIL(Feature.MAILBOX, R.id.mailFragment, MailOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            val rid = request.addGetEmailStateRequest()
            return listOf(rid)
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>, apiContext: IApiContext): AbstractOverviewElement {
            val subResponse = response.values.toList().single()
            val quota = WebWeaverClient.json.decodeFromJsonElement<Quota>(subResponse["quota"]!!)
            val unread = subResponse["unread_messages"]!!.jsonPrimitive.int
            return MailOverview(quota, unread)
        }
    }, "overview_show_mail"),
    FEATURE_FILE_STORAGE(Feature.FILES, R.id.fileStorageGroupFragment, FileStorageOverview::class.java, object: OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            val rid = request.addGetFileStorageStateRequest()
            return listOf(rid)
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>, apiContext: IApiContext): AbstractOverviewElement {
            val subResponse = response.values.toList().single()
            val quota = WebWeaverClient.json.decodeFromJsonElement<Quota>(subResponse["quota"]!!)
            return FileStorageOverview(quota)
        }
    }, "overview_show_filestorage"),
    FEATURE_NOTIFICATIONS(Feature.BOARD, R.id.notificationsFragment, NotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            return request.addGetAllBoardNotificationsRequest(user)
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>, apiContext: IApiContext): AbstractOverviewElement {
            var count = 0
            response.values.forEach { subResponse ->
                count += subResponse["entries"]!!.jsonArray.size
            }
            return NotificationsOverview(count)
        }
    }, "overview_show_notifications"),
    FEATURE_FORUM(Feature.FORUM, R.id.forumGroupFragment),
    FEATURE_MEMBERS(Feature.MEMBERS, R.id.membersGroupFragment, GroupsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            return emptyList() // not needed
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>, apiContext: IApiContext): AbstractOverviewElement {
            return GroupsOverview(apiContext.user.getGroups().size)
        }
    }, "overview_show_groups"),
    FEATURE_SYSTEM_NOTIFICATIONS(Feature.MESSAGES, R.id.systemNotificationsFragment, SystemNotificationsOverview::class.java, object : OverviewBuilder {
        override fun appendRequests(request: UserApiRequest, user: IUser): List<Int> {
            val rid = request.addGetSystemNotificationsRequest()
            return listOf(rid)
        }

        override fun createElementFromResponse(response: Map<Int, JsonObject>, apiContext: IApiContext): AbstractOverviewElement {
            val subResponse = response.values.toList().single()
            val systemNotifications = mutableListOf<SystemNotification>()
            subResponse["messages"]!!.jsonArray.forEach { messageResponse ->
                systemNotifications.add(WebWeaverClient.json.decodeFromJsonElement(messageResponse))
            }
            return SystemNotificationsOverview(systemNotifications.count { it.isUnread })
        }
    }, "overview_show_systemnotifications"),
    FEATURE_MESSENGER(Feature.MESSENGER, R.id.chatsFragment),
    FEATURE_CONTACTS(Feature.ADDRESSES, R.id.contactsGroupFragment),
    FEATURE_NOTES(Feature.NOTES, R.id.notesFragment);

    companion object {
        fun getByOverviewClass(overviewClass: Class<out AbstractOverviewElement>): AppFeature? {
            return values().firstOrNull { it.overviewClass == overviewClass }
        }
    }

}