package de.deftk.openlonet.feature.filestorage

import android.app.AuthenticationRequiredException
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.*
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import android.util.Patterns
import androidx.preference.PreferenceManager
import de.deftk.lonet.api.LoNet
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.Permission
import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.feature.files.OnlineFile
import de.deftk.lonet.api.model.feature.files.filters.FileFilter
import de.deftk.openlonet.AuthStore
import de.deftk.openlonet.R
import de.deftk.openlonet.activities.LoginActivity
import de.deftk.openlonet.utils.FileUtil
import kotlinx.coroutines.*
import java.net.URL

class ApiDocumentsProvider: DocumentsProvider() {

    //TODO invalidate caches when failed to find document

    companion object {
        private const val LOG_TAG = "ApiDocumentsProvider"

        private const val ROOT_ID = "ROOT_1"
        private const val ROOT_FOLDER_ID = "ROOT_FOLDER_1"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )
    }

    private val cache = DocumentCache()
    private val handler: Handler

    init {
        val thread = LooperThread()
        thread.start()
        handler = Handler(thread.looper)
    }

    override fun onCreate(): Boolean {
        Log.i(LOG_TAG, "onCreate()")
        return AuthStore.getSavedToken(acquireContext()) != null
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        Log.i(LOG_TAG, "queryRoots(projection=$projection)")
        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            with(newRow()) {
                add(Root.COLUMN_ROOT_ID, ROOT_ID)
                add(Root.COLUMN_DOCUMENT_ID, ROOT_FOLDER_ID)
                add(Root.COLUMN_SUMMARY, null)
                add(Root.COLUMN_TITLE, acquireContext().getString(R.string.app_name))
                add(Root.COLUMN_FLAGS, 0)
                add(Root.COLUMN_MIME_TYPES, "*/*")
                add(Root.COLUMN_ICON, R.drawable.ic_launcher_foreground)
            }
        }
    }

    @Throws(SecurityException::class)
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        Log.i(LOG_TAG, "queryDocument(documentId=$documentId, projection=$projection)")
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)

        when {
            documentId == ROOT_FOLDER_ID -> {
                with(cursor.newRow()) {
                    add(Document.COLUMN_DOCUMENT_ID, ROOT_FOLDER_ID)
                    add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
                    add(Document.COLUMN_DISPLAY_NAME, acquireContext().getString(R.string.app_name))
                    add(Document.COLUMN_LAST_MODIFIED, null)
                    add(Document.COLUMN_FLAGS, 0)
                    add(Document.COLUMN_SIZE, null)
                }
            }
            Patterns.EMAIL_ADDRESS.matcher(documentId).matches() -> {
                val operator = AuthStore.getAppUser().getContext().getOperator(documentId)
                if (operator != null) {
                    includeOperator(cursor, operator)
                } else {
                    Log.e(LOG_TAG, "Request operator $documentId could not be found")
                }
            }
            documentId.contains(":") -> {
                val operatorId = documentId.split(":")[0]
                val fileId = documentId.split(":", limit = 2)[1]
                val parentId = buildDocumentId(operatorId, fileId.getParentPath())
                val parentNotifyUri = buildNotifyUri(parentId)
                val cached = cache.get(parentNotifyUri)
                if (cached == null) {
                    Log.w(LOG_TAG, "Document not in cache, fetching from network")
                    return getLoadingCursor(projection).apply {
                        setNotificationUri(acquireContext().contentResolver, parentNotifyUri)
                    }.also {
                        GlobalScope.launch {
                            queryChildDocumentsFromNetwork(
                                parentId,
                                null,
                                null,
                                buildNotifyUri(parentId)
                            )
                        }
                    }
                } else {
                    val file = cached
                        .map { it as OnlineFile }
                        .firstOrNull { it.id == fileId && it.operator.getLogin() == operatorId }
                    if (file != null) {
                        includeFile(cursor, file)
                    } else {
                        Log.e(LOG_TAG, "Requested document $documentId not found")
                    }
                }
            }
            else -> Log.e(LOG_TAG, "Unknown document id: $documentId")
        }

        return cursor
    }

    @Throws(SecurityException::class)
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor {
        Log.i(LOG_TAG, "queryChildDocuments(parentDocumentId=$parentDocumentId, projection=$projection, sortOrder=$sortOrder)")

        val notifyUri = buildNotifyUri(parentDocumentId)
        val cached = cache.get(notifyUri)
        return if (cached == null) {
            Log.i(LOG_TAG, "Documents not in cache, fetching from network")
            getLoadingCursor(projection).apply {
                setNotificationUri(acquireContext().contentResolver, notifyUri)
            }.also {
                GlobalScope.launch {
                    queryChildDocumentsFromNetwork(
                        parentDocumentId,
                        sortOrder,
                        null,
                        notifyUri
                    )
                }
            }
        } else {
            Log.i(LOG_TAG, "Documents in cache, returning")
            MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).also { cursor ->
                cached.forEach { file ->
                    when (file) {
                        is OnlineFile -> includeFile(cursor, file)
                        is AbstractOperator -> includeOperator(cursor, file)
                        else -> Log.e(LOG_TAG, "Unknown IFilePrimitive instance: ${file::class.java.name}")
                    }
                }
            }
        }
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor? {
        Log.i(LOG_TAG, "openDocument(documentId=$documentId, mode=$mode, signal=$signal)")

        val file = getCachedFileById(documentId)
        if (file != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageManager = acquireContext().getSystemService(StorageManager::class.java)
                return storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.parseMode(mode),
                    FileDescriptorCallback(signal) { file.getTempDownloadUrl() },
                    handler
                )
            } else {
                val pipes = ParcelFileDescriptor.createReliablePipe()
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val download = file.getTempDownloadUrl()
                            val out = ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])
                            val stream = URL(download.downloadUrl).openStream()
                            val buffer = ByteArray(1024)

                            var actualRead = 0
                            while (actualRead < download.size) {
                                if (signal?.isCanceled == true)
                                    break
                                val read = stream.read(buffer, 0, buffer.size)
                                if (read <= 0) break
                                out.write(buffer, 0, buffer.size)
                                actualRead += read
                            }
                            stream.close()
                            out.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                return pipes[0]
            }
        } else {
            Log.e(LOG_TAG, "Failed to show document (document not found inside cache)")
            return null
        }
    }

    override fun openDocumentThumbnail(documentId: String, sizeHint: Point?, signal: CancellationSignal?): AssetFileDescriptor? {
        Log.i(LOG_TAG, "openDocumentThumbnail(documentId=$documentId, sizeHint=$sizeHint, signal=$signal)")

        val file = getCachedFileById(documentId)
        if (file != null && file.preview == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageManager = acquireContext().getSystemService(StorageManager::class.java)
                val pfd = storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.MODE_READ_ONLY,
                    FileDescriptorCallback(signal) { file.getPreviewDownloadUrl() },
                    handler
                )
                return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
            } else {
                val pipes = ParcelFileDescriptor.createReliablePipe()
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val preview = file.getPreviewDownloadUrl()
                            val out = ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])
                            val stream = URL(preview.downloadUrl).openStream()
                            val buffer = ByteArray(1024)

                            var actualRead = 0
                            while (actualRead < preview.size) {
                                if (signal?.isCanceled == true)
                                    break
                                val read = stream.read(buffer, 0, buffer.size)
                                if (read <= 0) break
                                out.write(buffer, 0, buffer.size)
                                actualRead += read
                            }
                            stream.close()
                            out.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                return AssetFileDescriptor(pipes[0], 0, AssetFileDescriptor.UNKNOWN_LENGTH)
            }
        } else {
            Log.e(LOG_TAG, "Failed to show thumbnail (document not found inside cache)")
            return null
        }
    }

    override fun getDocumentType(documentId: String): String {
        val cached = getCachedFileById(documentId)
        return if (cached != null) {
            FileUtil.getMimeType(cached.name)
        } else {
            FileUtil.getMimeType(documentId.substring(documentId.lastIndexOf('/')))
        }
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    override fun refresh(uri: Uri, extras: Bundle?, cancellationSignal: CancellationSignal?): Boolean {
        cache.clear()
        notifyUri(uri)
        return true
    }

    @Throws(SecurityException::class)
    private fun queryChildDocumentsFromNetwork(parentDocumentId: String, sortOrder: String?, filter: FileFilter?, notifyUri: Uri) {
        Log.i(LOG_TAG, "queryChildDocumentsFromNetwork(parentDocumentId=$parentDocumentId, sortOrder=$sortOrder, filter=$filter, notifyUri=$notifyUri)")

        val sessionValid = try {
            AuthStore.getAppUser().checkSession()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } // ?: return

        if (!AuthStore.isUserLoggedIn() || sessionValid != true) {
            try {
                // try silent login
                val username = AuthStore.getSavedUsername(acquireContext()) ?: error("No username")
                val token = AuthStore.getSavedToken(acquireContext()) ?: error("No token")
                AuthStore.setAppUser(LoNet.loginToken(username, token))
            } catch (e: Exception) {
                e.printStackTrace()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // user should perform the login
                    //TODO verify functionality
                    val intent = Intent(acquireContext(), LoginActivity::class.java)
                    val action = PendingIntent.getActivity(acquireContext(), 1, intent, 0)
                    throw AuthenticationRequiredException(e, action)
                } else {
                    // send a signal that login failed
                    throw SecurityException()
                }
            }
        }

        when {
            parentDocumentId == ROOT_FOLDER_ID -> {
                val documents = mutableListOf<AbstractOperator>()
                if (Feature.FILES.isAvailable(AuthStore.getAppUser().effectiveRights))
                    documents.add(AuthStore.getAppUser())
                documents.addAll(AuthStore.getAppUser().groups.filter { Feature.FILES.isAvailable(it.effectiveRights) })
                cache.put(notifyUri, documents)
            }
            Patterns.EMAIL_ADDRESS.matcher(parentDocumentId).matches() -> {
                val rootCache = cache.get(buildNotifyUri(ROOT_FOLDER_ID))
                val operator = rootCache
                    ?.map { it as AbstractOperator }
                    ?.firstOrNull { it.getLogin() == parentDocumentId }
                if (operator != null) {
                    cache.put(notifyUri, operator.getFiles(filter))
                } else {
                    if (rootCache != null) {
                        Log.e(LOG_TAG, "Requested operator $parentDocumentId not found inside cache")
                        cache.put(notifyUri, emptyList()) // prevent infinite loop
                    } else {
                        Log.w(LOG_TAG, "Root cache is empty but required. So let's generate it")
                        queryChildDocumentsFromNetwork(
                            ROOT_FOLDER_ID,
                            null,
                            null,
                            buildNotifyUri(ROOT_FOLDER_ID)
                        )
                        queryChildDocumentsFromNetwork(
                            parentDocumentId,
                            sortOrder,
                            filter,
                            notifyUri
                        )
                    }
                }
            }
            parentDocumentId.contains(":") -> {
                val operatorId = parentDocumentId.split(":")[0]
                val fileId = parentDocumentId.split(":", limit = 2)[1]
                val parentCache = cache.get(
                    buildNotifyUri(buildDocumentId(operatorId, fileId.getParentPath()))
                ) ?: emptyList()
                val parent = parentCache
                    .map { it as OnlineFile }
                    .firstOrNull { it.id == fileId && it.operator.getLogin() == operatorId }
                if (parent != null) {
                    cache.put(notifyUri, parent.getFiles(filter))
                } else {
                    Log.e(LOG_TAG, "Requested document $parentDocumentId not found")
                    cache.put(notifyUri, emptyList()) // prevent infinite loop
                }
            }
            else -> {
                Log.e(LOG_TAG, "Unknown parentDocumentId format: $parentDocumentId")
                cache.put(notifyUri, emptyList()) // prevent infinite loop
            }
        }
        notifyUri(notifyUri)
    }

    /**
     * Returns the cached document object expected to be received from previous web requests.
     * This method doesn't perform actual web requests.
     * @param documentId: Id of the document to resolve
     * @return Cached document or null if not found in cache
     */
    private fun getCachedFileById(documentId: String): OnlineFile? {
        val operatorId = documentId.split(":")[0]
        val fileId = documentId.split(":", limit = 2)[1]
        val parentId = buildDocumentId(operatorId, fileId.getParentPath())
        val parentNotifyUri = buildNotifyUri(parentId)
        val cached = cache.get(parentNotifyUri)
        if (cached == null) {
            Log.w(LOG_TAG, "Document $documentId was not found inside the cache")
            return null
        } else {
            val file = cached
                .map { it as OnlineFile }
                .firstOrNull { it.id == fileId && it.operator.getLogin() == operatorId }
            if (file != null) {
                return file
            } else {
                Log.e(LOG_TAG, "Requested document $documentId not found although parent was cached earlier")
            }
        }
        return null
    }

    /**
     * @param operatorId: Login of user or group holding the file
     * @param fileId: Unique id of the file (may be empty)
     * @return DocumentId (independent of the operator)
     */
    private fun buildDocumentId(operatorId: String, fileId: String): String {
        return if (fileId.isEmpty())
            operatorId
        else
            "$operatorId:$fileId"
    }

    /**
     * @param parentDocumentId: Document whose query operation takes longer and needs callback
     * functionality
     * @return NotifyUri for the given parentDocumentId
     */
    private fun buildNotifyUri(parentDocumentId: String): Uri {
        return DocumentsContract.buildChildDocumentsUri(
            "de.deftk.openlonet.filestorage.fileprovider",
            parentDocumentId
        )
    }

    /**
     * @param projection: Column names provided when creating cursor the cursor
     * @return A cursor signaling an operation is pending. NotifyUri will be called if the
     * operation has finished
     */
    private fun getLoadingCursor(projection: Array<out String>?): MatrixCursor {
        return object : MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION) {
            override fun getExtras(): Bundle {
                return Bundle().apply {
                    putBoolean(DocumentsContract.EXTRA_LOADING, true)
                }
            }
        }
    }

    /**
     * @return The context of this DocumentsProvider or raises an error if the context is null
     */
    private fun acquireContext(): Context {
        return context ?: error("No context")
    }

    /**
     * Adds data of given operator to a new row in the given cursor
     * @param cursor: Cursor to add new row with data to
     * @param operator: Data source
     */
    private fun includeOperator(cursor: MatrixCursor, operator: AbstractOperator) {
        var flags = 0
        if (operator.effectiveRights.contains(Permission.FILES_WRITE)
            || operator.effectiveRights.contains(Permission.FILES_ADMIN)) {
            flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        }

        val row = cursor.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, operator.getLogin())
        row.add(Document.COLUMN_DISPLAY_NAME, operator.getName())
        row.add(Document.COLUMN_SIZE, -1)
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
        row.add(Document.COLUMN_LAST_MODIFIED, 0)
        row.add(Document.COLUMN_FLAGS, flags)
        row.add(Document.COLUMN_ICON, R.drawable.ic_launcher_foreground)
    }

    /**
     * Adds data of given file to a new row in the given cursor
     * @param cursor: Cursor to add new row with data to
     * @param file: Data source
     */
    private fun includeFile(cursor: MatrixCursor, file: OnlineFile) {
        var flags = 0
        if (file.effectiveWrite == true) {
            flags = if (file.type == OnlineFile.FileType.FOLDER) {
                flags or Document.FLAG_DIR_SUPPORTS_CREATE
            } else {
                flags or Document.FLAG_SUPPORTS_WRITE
            }
        }
        if (file.effectiveDelete == true) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE
        }
        if (file.preview == true && shouldShowThumbnail()) {
            //FIXME some images cause weird behaviour of document chooser activity (documents don't show up anymore, ...)
            // this is probably related to some other stuff (?)
            flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL
        }

        val row = cursor.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, "${file.operator.getLogin()}:${file.id}")
        row.add(Document.COLUMN_DISPLAY_NAME, file.name)
        row.add(Document.COLUMN_SIZE, file.size)
        if (file.type == OnlineFile.FileType.FILE) {
            row.add(Document.COLUMN_MIME_TYPE, FileUtil.getMimeType(file.name))
        } else {
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
        }
        row.add(Document.COLUMN_LAST_MODIFIED, file.modificationDate.time)
        row.add(Document.COLUMN_FLAGS, flags)
        row.add(Document.COLUMN_ICON, R.drawable.ic_launcher_foreground)
    }

    /**
     * @return True if user preference is enabled
     */
    private fun shouldShowThumbnail() = PreferenceManager.getDefaultSharedPreferences(acquireContext()).getBoolean("file_storage_integration_enable_preview_images", false)

    /**
     * Send change signal with given uri
     */
    private fun notifyUri(notifyUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            acquireContext().contentResolver.notifyChange(
                notifyUri,
                null,
                ContentResolver.NOTIFY_SYNC_TO_NETWORK
            )
        } else {
            @Suppress("DEPRECATION")
            acquireContext().contentResolver.notifyChange(
                notifyUri,
                null,
                false
            )
        }
    }

    /**
     * @return Parent folder id of the given file/folder or an empty string if the given id
     * is the root object "/" or id invalid
     */
    private fun String.getParentPath(): String {
        val end = lastIndexOf('/')
        if (end == -1)
            return ""
        return substring(0, end)
    }

    class LooperThread : HandlerThread("ApiDocumentsProviderHandlerThread")
}