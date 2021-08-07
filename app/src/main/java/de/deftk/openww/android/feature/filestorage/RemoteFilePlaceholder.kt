package de.deftk.openww.android.feature.filestorage

import de.deftk.openww.api.model.IRequestContext
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.feature.FileDownloadUrl
import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.Modification
import de.deftk.openww.api.model.feature.filestorage.DownloadNotification
import de.deftk.openww.api.model.feature.filestorage.FileAggregation
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.model.feature.filestorage.filter.FileFilter
import de.deftk.openww.api.model.feature.filestorage.io.FileChunk
import de.deftk.openww.api.model.feature.filestorage.proxy.ProxyNonce
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile

class RemoteFilePlaceholder(
    override val id: String,
    override val name: String,
    override val size: Long,
    override val parentId: String,
    modification: Modification
): IRemoteFile {

    override val created: Modification = modification
    override val modified: Modification = modification
    override val aggregation: FileAggregation? = null
    override val description: String? = null
    override val downloadNotification: DownloadNotification? = null
    override val effectiveCreate: Boolean? = null
    override val effectiveDelete: Boolean? = null
    override val effectiveModify: Boolean? = null
    override val effectiveRead: Boolean? = null
    override val empty: Boolean? = null
    override val mine: Boolean = true
    override val preview: Boolean = false
    override val readable: Boolean = false
    override val shared: Boolean = false
    override val sparse: Boolean? = null
    override val sparseKey: String? = null
    override val type: FileType = FileType.FILE
    override val writable: Boolean = false
    override val ordinal: Int? = null

    override suspend fun delete(context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun download(limit: Int?, offset: Int?, context: IRequestContext): FileChunk {
        throw IllegalStateException("Not supported")
    }

    override suspend fun exportSessionFile(user: IUser, context: IRequestContext): ISessionFile {
        throw IllegalStateException("Not supported")
    }

    override suspend fun getDownloadUrl(context: IRequestContext): FileDownloadUrl {
        throw IllegalStateException("Not supported")
    }

    override suspend fun getPreviewUrl(context: IRequestContext): FilePreviewUrl {
        throw IllegalStateException("Not supported")
    }

    override suspend fun getProxyNonce(context: IRequestContext): ProxyNonce {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setDescription(description: String, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setDownloadNotificationAddLogin(login: String, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setDownloadNotificationDeleteLogin(login: String, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setDownloadNotificationMe(receive: Boolean, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setName(name: String, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setUploadNotificationAddLogin(login: String, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setUploadNotificationDeleteLogin(login: String, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setUploadNotificationMe(receive: Boolean, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun addFile(name: String, data: ByteArray, description: String?, context: IRequestContext): IRemoteFile {
        throw IllegalStateException("Not supported")
    }

    override suspend fun addFolder(name: String, description: String?, context: IRequestContext): IRemoteFile {
        throw IllegalStateException("Not supported")
    }

    override suspend fun addSparseFile(name: String, size: Int, description: String?, context: IRequestContext): IRemoteFile {
        throw IllegalStateException("Not supported")
    }

    override suspend fun getFiles(limit: Int?, offset: Int?, filter: FileFilter?, context: IRequestContext): List<IRemoteFile> {
        throw IllegalStateException("Not supported")
    }

    override suspend fun getRootFile(context: IRequestContext): IRemoteFile {
        throw IllegalStateException("Not supported")
    }

    override suspend fun importSessionFile(sessionFile: ISessionFile, createCopy: Boolean?, description: String?, context: IRequestContext): IRemoteFile {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setReadable(readable: Boolean, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }

    override suspend fun setWritable(writable: Boolean, context: IRequestContext) {
        throw IllegalStateException("Not supported")
    }
}