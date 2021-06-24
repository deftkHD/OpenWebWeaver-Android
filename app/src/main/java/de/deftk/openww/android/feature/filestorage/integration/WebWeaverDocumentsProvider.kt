package de.deftk.openww.android.feature.filestorage.integration

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.*
import android.os.storage.StorageManager
import android.provider.DocumentsContract.*
import android.provider.DocumentsProvider
import android.util.Log
import android.util.Patterns
import androidx.preference.PreferenceManager
import de.deftk.openww.android.R
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.utils.FileUtil
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.auth.Credentials
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.implementation.OperatingScope
import de.deftk.openww.api.implementation.feature.filestorage.RemoteFile
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.Permission
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.request.handler.AutoLoginRequestHandler
import kotlinx.coroutines.*
import java.net.URL

class WebWeaverDocumentsProvider: DocumentsProvider() {

    //TODO implement sorting
    //TODO implement refreshing only one directory
    //TODO implement write actions

    companion object {
        private val TAG = WebWeaverDocumentsProvider::class.java.name

        private const val DELIMITER = "$"
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

    private val cache = ProviderCache()
    private val fileStorage = FileStorage()
    private val handler: Handler
    private var apiContext: ApiContext? = null

    private lateinit var accounts: List<Account>

    init {
        val thread = LooperThread()
        thread.start()
        handler = Handler(thread.looper)
    }

    override fun onCreate(): Boolean {
        Log.i(TAG, "onCreate()")
        accounts = AuthHelper.findAccounts(null, context()).toList()
        return accounts.isNotEmpty()
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        Log.i(TAG, "queryRoots(projection=$projection)")
        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            with(newRow()) {
                add(Root.COLUMN_ROOT_ID, ROOT_ID)
                add(Root.COLUMN_DOCUMENT_ID, ROOT_FOLDER_ID)
                add(Root.COLUMN_SUMMARY, null)
                add(Root.COLUMN_TITLE, context().getString(R.string.app_name))
                add(Root.COLUMN_FLAGS, 0)
                add(Root.COLUMN_MIME_TYPES, "*/*")
                add(Root.COLUMN_ICON, R.drawable.ic_launcher_foreground)
            }
        }
    }

    // description of a specific entry/document
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        Log.i(TAG, "queryDocument(documentId=$documentId, projection=$projection)")
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        when {
            documentId == ROOT_FOLDER_ID -> {
                with(cursor.newRow()) {
                    add(Document.COLUMN_DOCUMENT_ID, ROOT_FOLDER_ID)
                    add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
                    add(Document.COLUMN_DISPLAY_NAME, context().getString(R.string.app_name))
                    add(Document.COLUMN_LAST_MODIFIED, null)
                    add(Document.COLUMN_FLAGS, 0)
                    add(Document.COLUMN_SIZE, null)
                }
            }
            Patterns.EMAIL_ADDRESS.matcher(documentId).matches() -> {
                // webweaver account
                if (apiContext?.getUser()?.login != documentId) {
                    val callbackUri = buildCallbackUri(documentId)
                    return getLoadingCursor(projection).apply {
                        setNotificationUri(context().contentResolver, callbackUri)
                    }.also {
                        GlobalScope.launch {
                            login(documentId)
                            notifyDataChange(callbackUri)
                        }
                    }
                } else {
                    includeAccount(cursor, accounts.first { it.name == documentId })
                }
            }
            documentId.contains(DELIMITER) -> {
                // file/folder
                val operatorId = documentId.split(DELIMITER, limit = 2)[0]
                val fileId = documentId.split(DELIMITER, limit = 2)[1]
                val parentId = buildDocumentId(operatorId, getParentPath(fileId))
                val parentNotifyUri = buildCallbackUri(parentId)
                val cached = cache.get(parentNotifyUri)
                if (cached == null) {
                    Log.w(TAG, "Document not in cache, fetching from network")
                    return getLoadingCursor(projection).apply {
                        setNotificationUri(context().contentResolver, parentNotifyUri)
                    }.also {
                        GlobalScope.launch {
                            queryChildrenFromNetwork(parentId, null, buildCallbackUri(parentId))
                        }
                    }
                } else {
                    val file = cached.firstOrNull { (it.provider as IRemoteFile).id == fileId && it.scope.name == operatorId }
                    if (file != null) {
                        includeFile(cursor, file.provider as IRemoteFile, file.scope)
                    } else {
                        Log.e(TAG, "Requested document $documentId not found")
                    }
                }
            }
            !documentId.contains("/") && !documentId.contains(DELIMITER) -> {
                // scope
                val scope = findOperatingScope(documentId, apiContext()) ?: error("Unknown or invalid scope: $documentId")
                includeScope(cursor, scope)
            }
            else -> Log.e(TAG, "Unknown or invalid documentId: $documentId")
        }
        return cursor
    }

    // children of a directory/document
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor {
        Log.i(TAG, "queryChildDocuments(parentDocumentId=$parentDocumentId, projection=$projection, sortOrder=$sortOrder)")

        val callbackUri = buildCallbackUri(parentDocumentId)
        val cached = cache.get(callbackUri)
        return if (cached == null) {
            Log.i(TAG, "Documents not in cache, fetching from network")
            if (parentDocumentId == ROOT_FOLDER_ID && accounts.size > 1) {
                val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
                accounts.forEach { account ->
                    includeAccount(cursor, account)
                }
                cursor
            } else {
                getLoadingCursor(projection).apply {
                    setNotificationUri(context().contentResolver, callbackUri)
                }.also {
                    GlobalScope.launch {
                        queryChildrenFromNetwork(parentDocumentId, sortOrder, callbackUri)
                    }
                }
            }

        } else {
            Log.i(TAG, "Documents in cache")
            MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).also { cursor ->
                cached.forEach { cacheElement ->
                    when (cacheElement.provider) {
                        is IRemoteFile -> includeFile(cursor, cacheElement.provider, cacheElement.scope)
                        is IOperatingScope -> includeScope(cursor, cacheElement.scope)
                        else -> Log.e(TAG, "Unknown IFileProvider instance: ${cacheElement.provider::class.java.name}")
                    }
                }
            }
        }
    }

    // download
    override fun openDocument(documentId: String, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor? {
        Log.i(TAG, "openDocument(documentId=$documentId, mode=$mode, signal=$signal)")

        val provider = getCachedProvider(documentId)
        if (provider != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageManager = context().getSystemService(StorageManager::class.java)
                return storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.parseMode(mode),
                    FileDescriptorReadCallback(signal) { (provider.provider as IRemoteFile).getDownloadUrl(provider.scope.getRequestContext(apiContext())) },
                    handler
                )
            } else {
                val pipes = ParcelFileDescriptor.createReliablePipe()
                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val download = (provider.provider as IRemoteFile).getDownloadUrl(provider.scope.getRequestContext(apiContext()))
                            val out = ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])
                            val stream = URL(download.url).openStream()
                            val buffer = ByteArray(1024)

                            var actualRead = 0
                            while (actualRead < download.size ?: -1) {
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
            Log.e(TAG, "Failed to show document $documentId (document not found inside cache)")
            return null
        }
    }

    override fun openDocumentThumbnail(documentId: String, sizeHint: Point?, signal: CancellationSignal?): AssetFileDescriptor? {
        Log.i(TAG, "openDocumentThumbnail(documentId=$documentId, sizeHint=$sizeHint, signal=$signal)")

        val provider = getCachedProvider(documentId)
        val file = provider?.provider as? RemoteFile?
        if (provider != null && file?.hasPreview() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageManager = context().getSystemService(StorageManager::class.java)
                val pfd = storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.MODE_READ_ONLY,
                    FileDescriptorReadCallback(signal) { file.getPreviewUrl(provider.scope.getRequestContext(apiContext())) },
                    handler
                )
                return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
            } else {
                val pipes = ParcelFileDescriptor.createReliablePipe()
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val preview = file.getPreviewUrl(provider.scope.getRequestContext(apiContext()))
                            val out = ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])
                            val stream = URL(preview.url).openStream()
                            val buffer = ByteArray(1024)

                            var actualRead = 0
                            while (actualRead < (preview.size ?: 0)) {
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
            Log.e(TAG, "Failed to show thumbnail (document not found inside cache)")
            return null
        }
    }

    override fun getDocumentType(documentId: String): String {
        val cached = getCachedProvider(documentId)
        return if (cached != null) {
            FileUtil.getMimeType(cached.provider.name)
        } else {
            FileUtil.getMimeType(documentId.substring(documentId.lastIndexOf('/')))
        }
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    private suspend fun queryChildrenFromNetwork(providerId: String, sortOrder: String?, callbackUri: Uri) {
        Log.i(TAG, "queryChildrenFromNetwork(providerId=$providerId, sortOrder=$sortOrder, callbackUri=$callbackUri)")
        when {
            providerId == ROOT_FOLDER_ID -> {
                if (accounts.size == 1 && apiContext != null) {
                    // put groups/scopes
                    cache.put(callbackUri, fileStorage.getScopes(apiContext!!).map { ProviderCacheElement(it, it) })
                }
            }
            Patterns.EMAIL_ADDRESS.matcher(providerId).matches() -> {
                // webweaver account
                login(providerId)
                cache.put(callbackUri, fileStorage.getScopes(apiContext()).map { ProviderCacheElement(it, it) })
            }
            providerId.contains(DELIMITER) -> {
                // file/folder
                val operatorId = providerId.split(DELIMITER, limit = 2)[0]
                val fileId = providerId.split(DELIMITER, limit = 2)[1]
                val parentCache = cache.get(buildCallbackUri(buildDocumentId(operatorId, getParentPath(fileId)))) ?: emptyList()
                val parent = parentCache.firstOrNull { (it.provider as IRemoteFile).id == fileId && it.scope.name == operatorId }
                if (parent != null) {
                    val response = withContext(Dispatchers.IO) {
                        parent.provider.getFiles(context = parent.scope.getRequestContext(apiContext()))
                    }
                    cache.put(callbackUri, response.map { ProviderCacheElement(parent.scope, it) })
                } else {
                    Log.e(TAG, "Requested provider $providerId not found")
                    cache.dump()
                    cache.put(callbackUri, emptyList())
                }
            }
            !providerId.contains("/") && !providerId.contains(DELIMITER) -> {
                // scope
                val scope = findOperatingScope(providerId, apiContext()) ?: error("Unknown or invalid scope: $providerId")
                val response = withContext(Dispatchers.IO) {
                    fileStorage.loadRootFiles(scope, apiContext())
                }
                cache.put(callbackUri, response.valueOrNull() ?: emptyList())
            }
            else -> {
                Log.e(TAG, "Unknown or invalid providerId: $providerId")
                cache.put(callbackUri, emptyList())
            }
        }
        notifyDataChange(callbackUri)
    }

    private fun context(): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireContext()
        } else {
            return context ?: error("No context")
        }
    }

    private fun apiContext(): ApiContext {
        return apiContext ?: error("ApiContext not available")
    }

    /**
     * Returns the cached document object expected to be received from previous web requests.
     * This method doesn't perform actual web requests.
     * @param documentId: Id of the document to resolve
     * @return Cached document or null if not found in cache
     */
    private fun getCachedProvider(documentId: String): ProviderCacheElement? {
        val operatorId = documentId.split(DELIMITER, limit = 2)[0]
        val fileId = try {
            documentId.split(DELIMITER, limit = 2)[1]
        } catch(e: Exception) {
            e.printStackTrace()
            error("Invalid documentId: $documentId")
        }
        val parentId = buildDocumentId(operatorId, getParentPath(fileId))
        val parentCallbackUri = buildCallbackUri(parentId)
        val cachedParent = cache.get(parentCallbackUri)
        if (cachedParent == null) {
            Log.w(TAG, "Parent of document $documentId was not found inside the cache")
            return null
        } else {
            val file = cachedParent.firstOrNull { (it.provider as IRemoteFile).id == fileId && it.scope.name == operatorId }
            if (file != null) {
                return file
            } else {
                Log.e(TAG, "Document $documentId was not found inside the cache")
            }
        }
        return null
    }

    /**
     * Adds data of given account to a new row in the given cursor
     * @param cursor: Cursor to add new row with data to
     * @param account: Data source
     */
    private fun includeAccount(cursor: MatrixCursor, account: Account) {
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, account.name)
            add(Document.COLUMN_DISPLAY_NAME, account.name.split("@")[0])
            add(Document.COLUMN_SIZE, -1)
            add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            add(Document.COLUMN_LAST_MODIFIED, 0)
            add(Document.COLUMN_FLAGS, 0)
            add(Document.COLUMN_ICON, R.drawable.ic_launcher_foreground)
        }
    }

    /**
     * Adds data of given scope to a new row in the given cursor
     * @param cursor: Cursor to add new row with data to
     * @param scope: Data source
     */
    private fun includeScope(cursor: MatrixCursor, scope: IOperatingScope) {
        var flags = 0
        if (scope.effectiveRights.contains(Permission.FILES_WRITE) || scope.effectiveRights.contains(Permission.FILES_ADMIN)) {
            flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        }

        //TODO try to include more data
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, scope.name)
            add(Document.COLUMN_DISPLAY_NAME, scope.name)
            add(Document.COLUMN_SIZE, -1)
            add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            add(Document.COLUMN_LAST_MODIFIED, 0)
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_ICON, R.drawable.ic_launcher_foreground)
        }
    }

    /**
     * Adds data of given file to a new row in the given cursor
     * @param cursor: Cursor to add new row with data to
     * @param scope: Scope of the file (e.g. personal -> IUser or group -> IGroup)
     * @param file: Data source
     */
    private fun includeFile(cursor: MatrixCursor, file: IRemoteFile, scope: IOperatingScope) {
        var flags = 0
        if (file.effectiveModify() == true) {
            flags = if (file.type == FileType.FOLDER) {
                flags or Document.FLAG_DIR_SUPPORTS_CREATE
            } else {
                flags or Document.FLAG_SUPPORTS_WRITE
            }
        }
        if (file.effectiveDelete() == true) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE
        }
        if (file.hasPreview() == true && shouldShowThumbnail()) {
            //FIXME some images cause weird behaviour of document chooser activity (documents don't show up anymore, ...)
            // this is probably related to some other stuff (?)
            flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL
        }

        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, buildDocumentId(scope.name, file.id))
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_SIZE, file.getSize())
            if (file.type == FileType.FILE) {
                add(Document.COLUMN_MIME_TYPE, FileUtil.getMimeType(file.name))
            } else {
                add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            }
            add(Document.COLUMN_LAST_MODIFIED, file.getModified().date.time)
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_ICON, R.drawable.ic_launcher_foreground)
        }
    }

    /**
     * Whether the setting is enabled (show thumbnails) or not (don't show thumbnails)
     * @return True if user preference is enabled
     */
    private fun shouldShowThumbnail() = PreferenceManager.getDefaultSharedPreferences(context()).getBoolean("file_storage_integration_enable_preview_images", false)

    /**
     * @param operatorId: Name of user or group holding the file (scope)
     * @param fileId: Unique id of the file (may be empty)
     * @return DocumentId (independent of the operator)
     */
    private fun buildDocumentId(operatorId: String, fileId: String): String {
        return if (fileId.isEmpty())
            operatorId
        else
            "$operatorId$DELIMITER$fileId"
    }

    /**
     * @param parentDocumentId: Document whose query operation takes longer and needs callback
     * functionality
     * @return uri for the given parentDocumentId
     */
    private fun buildCallbackUri(parentDocumentId: String) = buildChildDocumentsUri(
        "de.deftk.openww.android.filestorage.integration.fileprovider",
        parentDocumentId
    )

    /**
     * @param projection: Column names provided when creating cursor the cursor
     * @return A cursor signaling an operation is pending. notifyDataChange has to be called if the
     * operation has finished
     */
    private fun getLoadingCursor(projection: Array<out String>?): MatrixCursor {
        return object : MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION) {
            override fun getExtras(): Bundle {
                return Bundle().apply {
                    putBoolean(EXTRA_LOADING, true)
                }
            }
        }
    }

    /**
     * Notify about data is ready for given uri. The uri has to be used before (see getLoadingCursor)
     */
    private fun notifyDataChange(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context().contentResolver.notifyChange(
                uri,
                null,
                ContentResolver.NOTIFY_SYNC_TO_NETWORK
            )
        } else {
            @Suppress("DEPRECATION")
            context().contentResolver.notifyChange(
                uri,
                null,
                false
            )
        }
    }

    /**
     * @return Parent folder id of the given file/folder or an empty string if the given id
     * is the root object "/" or id invalid
     */
    private fun getParentPath(id: String): String {
        val end = id.lastIndexOf('/')
        if (end == -1)
            return ""
        return id.substring(0, end)
    }

    private fun findOperatingScope(name: String, apiContext: ApiContext): OperatingScope? {
        return apiContext.getUser().getGroups().firstOrNull { it.name == name } ?: if (name == apiContext.getUser().name) apiContext.getUser() else null
    }

    private suspend fun login(login: String) {
        val account = accounts.first { it.name == login }
        val accountManager = AccountManager.get(context())
        val token = accountManager.getPassword(account)
        apiContext = withContext(Dispatchers.IO) {
            WebWeaverClient.loginToken(account.name, token)
        }
        setupApiContext(apiContext!!, Credentials.fromToken(login, token))
    }

    private fun setupApiContext(apiContext: ApiContext, credentials: Credentials) {
        apiContext.setRequestHandler(AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override fun getCredentials(): Credentials = credentials

            override fun onLogin(context: ApiContext) {
                this@WebWeaverDocumentsProvider.apiContext = context
            }
        }, ApiContext::class.java))
    }

    class LooperThread : HandlerThread("WebWeaverDocumentsProviderHandlerThread")

}