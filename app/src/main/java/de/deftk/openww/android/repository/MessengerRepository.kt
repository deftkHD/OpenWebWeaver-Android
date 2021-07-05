package de.deftk.openww.android.repository

import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import javax.inject.Inject

class MessengerRepository @Inject constructor() : AbstractRepository() {

    suspend fun addChat(login: String, apiContext: ApiContext) = apiCall {
        apiContext.user.addChat(login, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun removeChat(login: String, apiContext: ApiContext) = apiCall {
        apiContext.user.removeChat(login, apiContext.user.getRequestContext(apiContext))
    }

    suspend fun getChats(apiContext: ApiContext) = apiCall {
        apiContext.user.getUsers(onlineOnly = false, context = apiContext.user.getRequestContext(apiContext))
    }

    suspend fun getHistory(apiContext: ApiContext) = apiCall {
        apiContext.user.getHistory(exportSessionFile = true, context = apiContext.user.getRequestContext(apiContext))
    }

    suspend fun sendMessage(login: String, sessionFile: ISessionFile?, text: String?, apiContext: ApiContext) = apiCall {
        apiContext.user.sendQuickMessage(login, sessionFile, text, apiContext.user.getRequestContext(apiContext))
    }

}