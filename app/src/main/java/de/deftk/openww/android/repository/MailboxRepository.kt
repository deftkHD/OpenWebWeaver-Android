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
        apiContext.user.getEmailFolders(apiContext.userContext())
    }

    suspend fun addFolder(name: String, apiContext: IApiContext): Response<IEmailFolder?> = apiCall {
        apiContext.user.addEmailFolder(name, apiContext.userContext())
        null
    }

    suspend fun deleteFolder(folder: IEmailFolder, apiContext: IApiContext) = apiCall {
        folder.delete(apiContext.userContext())
    }

    suspend fun getEmails(folder: IEmailFolder, apiContext: IApiContext) = apiCall {
        folder.getEmails(context = apiContext.userContext())
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
            apiContext.userContext()
        )
    }

    suspend fun readEmail(email: IEmail, folder: IEmailFolder, peek: Boolean? = null, apiContext: IApiContext) = apiCall {
        email.read(
            folder,
            peek,
            context = apiContext.userContext()
        )
        email
    }

    suspend fun setEmail(email: IEmail, folder: IEmailFolder, isFlagged: Boolean?, isUnread: Boolean?, apiContext: IApiContext) = apiCall {
        email.edit(folder, isFlagged ?: email.flagged, isUnread ?: email.unread, apiContext.userContext())
        email
    }

    suspend fun moveEmail(email: IEmail, src: IEmailFolder, dst: IEmailFolder, apiContext: IApiContext) = apiCall {
        email.move(src, dst, apiContext.userContext())
        email
    }

    suspend fun deleteEmail(email: IEmail, folder: IEmailFolder, apiContext: IApiContext) = apiCall {
        email.delete(folder, apiContext.userContext())
        email
    }

}