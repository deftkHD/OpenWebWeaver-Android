package de.deftk.openww.android.repository

import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.mailbox.IEmailFolder
import de.deftk.openww.android.api.Response
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import de.deftk.openww.api.model.feature.mailbox.ReferenceMode
import javax.inject.Inject

class MailboxRepository @Inject constructor() : AbstractRepository() {

    suspend fun getFolders(apiContext: IApiContext) = apiCall {
        //TODO sort
        apiContext.user.getEmailFolders(apiContext.user.getRequestContext(apiContext))
    }

    suspend fun addFolder(name: String, apiContext: IApiContext): Response<IEmailFolder?> = apiCall {
        apiContext.user.addEmailFolder(name, apiContext.user.getRequestContext(apiContext))
        null
    }

    suspend fun deleteFolder(folder: IEmailFolder, apiContext: IApiContext) = apiCall {
        folder.delete(apiContext.user.getRequestContext(apiContext))
    }

    suspend fun getEmails(folder: IEmailFolder, apiContext: IApiContext) = apiCall {
        folder.getEmails(context = apiContext.user.getRequestContext(apiContext))
    }

    suspend fun sendEmail(to: String, subject: String, plainBody: String, cc: String? = null, bcc: String? = null, importSessionFiles: List<ISessionFile>? = null, referenceFolderId: String? = null, referenceMessageId: Int? = null, referenceMode: ReferenceMode? = null, text: String? = null, apiContext: IApiContext) = apiCall {
        apiContext.user.sendEmail(
            to,
            subject,
            plainBody,
            null,
            cc,
            bcc,
            importSessionFiles,
            referenceFolderId,
            referenceMessageId,
            referenceMode,
            text,
            apiContext.user.getRequestContext(apiContext)
        )
    }

    suspend fun readEmail(email: IEmail, folder: IEmailFolder, peek: Boolean? = null, apiContext: IApiContext) = apiCall {
        email.read(
            folder,
            peek,
            context = apiContext.user.getRequestContext(apiContext)
        )
        email
    }

    suspend fun moveEmail(email: IEmail, src: IEmailFolder, dst: IEmailFolder, apiContext: IApiContext) = apiCall {
        email.move(src, dst, apiContext.user.getRequestContext(apiContext))
        email
    }

    suspend fun deleteEmail(email: IEmail, folder: IEmailFolder, apiContext: IApiContext) = apiCall {
        email.delete(folder, apiContext.user.getRequestContext(apiContext))
        email
    }

}