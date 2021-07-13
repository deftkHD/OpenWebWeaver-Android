package de.deftk.openww.android.repository

import de.deftk.openww.android.room.QuickMessageDao
import de.deftk.openww.android.room.RoomQuickMessage
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessengerRepository @Inject constructor(private val quickMessageDao: QuickMessageDao) : AbstractRepository() {

    suspend fun addChat(login: String, apiContext: ApiContext) = apiCall {
        apiContext.user.addChat(login, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun removeChat(login: String, apiContext: ApiContext) = apiCall {
        apiContext.user.removeChat(login, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun getChats(apiContext: ApiContext) = apiCall {
        apiContext.user.getUsers(onlineOnly = false, context = apiContext.user.getRequestContext(apiContext))
    }

    suspend fun getHistory(with: String, apiContext: ApiContext) = apiCall {
        val onlineMessages = apiContext.user.getHistory(exportSessionFile = true, context = apiContext.user.getRequestContext(apiContext))
            .filter { it.to.login == with || it.from.login == with }
            .toMutableList()
        withContext(Dispatchers.Unconfined) { // don't execute at network thread
            val savedMessages = quickMessageDao.getHistoryWith(with)
            val newMessages = onlineMessages.filter { receivedMessage -> savedMessages.none { it.id == receivedMessage.id } }
            quickMessageDao.insertMessages(newMessages.map { RoomQuickMessage.from(it) })
            if (savedMessages.size != onlineMessages.size) {
                // merge
                savedMessages.forEach { savedMessage ->
                    if (!onlineMessages.any { it.id == savedMessage.id }) {
                        onlineMessages.add(savedMessage.toQuickMessage())
                    }
                }
            }
        }

        onlineMessages.sortedBy { it.date.time }
    }

    suspend fun sendMessage(login: String, sessionFile: ISessionFile?, text: String?, apiContext: ApiContext) = apiCall {
        apiContext.user.sendQuickMessage(login, sessionFile, text, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun clearChat(login: String) {
        quickMessageDao.deleteMessages(quickMessageDao.getHistoryWith(login))
    }

}