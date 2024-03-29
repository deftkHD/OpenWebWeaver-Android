package de.deftk.openww.android.repository

import de.deftk.openww.android.feature.messenger.ChatContact
import de.deftk.openww.android.room.QuickMessageDao
import de.deftk.openww.android.room.RoomQuickMessage
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessengerRepository @Inject constructor(private val quickMessageDao: QuickMessageDao) : AbstractRepository() {

    suspend fun addChat(login: String, apiContext: IApiContext) = apiCall {
        apiContext.user.addChat(login, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun removeChat(login: String, apiContext: IApiContext) = apiCall {
        apiContext.user.removeChat(login, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun getChats(apiContext: IApiContext) = apiCall {
        val chats = apiContext.user.getUsers(onlineOnly = false, context = apiContext.user.getRequestContext(apiContext))
            .map { ChatContact(it, false) }
            .toMutableList()
        withContext(Dispatchers.Unconfined) {
            chats.addAll(getLocalChats(apiContext))
        }
        chats.distinctBy { it.user.login }.filter { it.user.login != apiContext.user.login }
    }

    suspend fun getLocalChats(apiContext: IApiContext): List<ChatContact> {
        val chats = mutableListOf<ChatContact>()
        val storedSenders = quickMessageDao.getAllSenders(apiContext.user.login)
        val storedRecipients = quickMessageDao.getAllRecipients(apiContext.user.login)
        chats.addAll(storedSenders.map { ChatContact(it, true) })
        chats.addAll(storedRecipients.map { ChatContact(it, true) })
        return chats.distinctBy { it.user.login }.filter { it.user.login != apiContext.user.login }
    }

    suspend fun getHistory(with: String, apiContext: IApiContext) = apiCall {
        val onlineMessages = apiContext.user.getHistory(exportSessionFile = true, context = apiContext.user.getRequestContext(apiContext))
            .filter { it.to.login == with || it.from.login == with }
            .toMutableList()
        withContext(Dispatchers.Unconfined) { // don't execute at network thread
            val savedMessages = quickMessageDao.getHistoryWith(apiContext.user.login, with)
            val newMessages = onlineMessages.filter { receivedMessage -> savedMessages.none { it.id == receivedMessage.id } }
            quickMessageDao.insertMessages(newMessages.map { RoomQuickMessage.from(apiContext.user.login, it) })
            if (savedMessages.size != onlineMessages.size) {
                // merge
                savedMessages.forEach { savedMessage ->
                    if (!onlineMessages.any { it.id == savedMessage.id }) {
                        onlineMessages.add(savedMessage.toQuickMessage())
                    }
                }
            }
        }

        onlineMessages
    }

    suspend fun sendMessage(login: String, sessionFile: ISessionFile?, text: String?, apiContext: IApiContext) = apiCall {
        apiContext.user.sendQuickMessage(login, sessionFile, text, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun clearChat(login: String, apiContext: IApiContext) {
        quickMessageDao.deleteMessages(quickMessageDao.getHistoryWith(apiContext.user.login, login))
    }

}