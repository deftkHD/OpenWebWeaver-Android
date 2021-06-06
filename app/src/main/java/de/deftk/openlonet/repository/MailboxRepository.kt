package de.deftk.openlonet.repository

import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.feature.mailbox.IEmail
import de.deftk.lonet.api.model.feature.mailbox.IEmailFolder
import de.deftk.openlonet.api.Response
import javax.inject.Inject

class MailboxRepository @Inject constructor() : AbstractRepository() {

    suspend fun getFolders(apiContext: ApiContext) = apiCall {
        //TODO sort
        apiContext.getUser().getEmailFolders(apiContext.getUser().getRequestContext(apiContext))
    }

    suspend fun addFolder(name: String, apiContext: ApiContext): Response<IEmailFolder?> = apiCall {
        apiContext.getUser().addEmailFolder(name, apiContext.getUser().getRequestContext(apiContext))
        null
    }

    suspend fun deleteFolder(folder: IEmailFolder, apiContext: ApiContext) = apiCall {
        folder.delete(apiContext.getUser().getRequestContext(apiContext))
    }

    suspend fun getEmails(folder: IEmailFolder, apiContext: ApiContext) = apiCall {
        folder.getEmails(context = apiContext.getUser().getRequestContext(apiContext)).sortedByDescending { it.getDate().time }
    }

    suspend fun readEmail(email: IEmail, folder: IEmailFolder, peek: Boolean? = null, apiContext: ApiContext) = apiCall {
        email.read(
            folder,
            peek,
            context = apiContext.getUser().getRequestContext(apiContext)
        )
        email
    }

    suspend fun moveEmail(email: IEmail, src: IEmailFolder, dst: IEmailFolder, apiContext: ApiContext) = apiCall {
        email.move(src, dst, apiContext.getUser().getRequestContext(apiContext))
        email
    }

    suspend fun deleteEmail(email: IEmail, folder: IEmailFolder, apiContext: ApiContext) = apiCall {
        email.delete(folder, apiContext.getUser().getRequestContext(apiContext))
        email
    }

}