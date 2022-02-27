package de.deftk.openww.android.viewmodel

import android.net.Uri
import androidx.lifecycle.*
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.feature.filestorage.*
import de.deftk.openww.android.filter.FileStorageFileFilter
import de.deftk.openww.android.filter.FileStorageQuotaFilter
import de.deftk.openww.android.repository.FileStorageRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.api.model.feature.Modification
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.model.feature.filestorage.IRemoteFileProvider
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FileStorageViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val fileStorageRepository: FileStorageRepository) : ScopedViewModel() {

    private val _quotas = MutableLiveData<Response<Map<IOperatingScope, Quota>>?>()
    val allQuotasResponse: LiveData<Response<Map<IOperatingScope, Quota>>?> = _quotas

    val quotaFilter = MutableLiveData(FileStorageQuotaFilter())
    val filteredQuotasResponse: LiveData<Response<Map<IOperatingScope, Quota>>?>
        get() = quotaFilter.switchMap { filter ->
            when (filter) {
                null -> allQuotasResponse
                else -> allQuotasResponse.switchMap { response ->
                    val filtered = MutableLiveData<Response<Map<IOperatingScope, Quota>>?>()
                    filtered.value = response?.smartMap { filter.apply(it.toList()).toMap() }
                    filtered
                }
            }
        }
    private val _files = mutableMapOf<IOperatingScope, MutableLiveData<Response<List<FileCacheElement>>?>>()
    val fileFilter = MutableLiveData(FileStorageFileFilter())
    private val _filteredFiles = mutableMapOf<IOperatingScope, LiveData<Response<List<FileCacheElement>>?>>()

    private val _batchDeleteResponse = MutableLiveData<List<Response<IRemoteFile>>?>()
    val batchDeleteResponse: LiveData<List<Response<IRemoteFile>>?> = _batchDeleteResponse

    private val _importSessionFileResponse = MutableLiveData<Response<IRemoteFile>?>()
    val importSessionFile: LiveData<Response<IRemoteFile>?> = _importSessionFileResponse

    private val _addFolderResponse = MutableLiveData<Response<IRemoteFile>?>()
    val addFolderResponse: LiveData<Response<IRemoteFile>?> = _addFolderResponse

    private val _networkTransfers = MutableLiveData<List<NetworkTransfer>>()
    val networkTransfers: LiveData<List<NetworkTransfer>> = _networkTransfers

    private val _editFileResponse = MutableLiveData<Response<IRemoteFile>?>()
    val editFileResponse: LiveData<Response<IRemoteFile>?> = _editFileResponse

    fun loadQuotas(apiContext: IApiContext) {
        viewModelScope.launch {
            _quotas.value = fileStorageRepository.getAllFileStorageQuotas(apiContext)
        }
    }

    fun getAllFiles(scope: IOperatingScope): LiveData<Response<List<FileCacheElement>>?> {
        return _files.getOrPut(scope) { MutableLiveData() }
    }

    fun getFilteredFiles(scope: IOperatingScope): LiveData<Response<List<FileCacheElement>>?> {
        return _filteredFiles.getOrPut(scope) {
            fileFilter.switchMap { filter ->
                when (filter) {
                    null -> getAllFiles(scope)
                    else -> getAllFiles(scope).switchMap { response ->
                        val filtered = MutableLiveData<Response<List<FileCacheElement>>>()
                        filtered.value = response?.smartMap { filter.apply(it.map { file -> file to scope }).map { pair -> pair.first } }
                        filtered
                    }
                }
            }
        }
    }

    fun loadChildren(scope: IOperatingScope, parentId: String, overwriteExisting: Boolean, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = fileStorageRepository.getFiles(parentId, true, scope, apiContext)
            val allFiles = getAllFiles(scope) as MutableLiveData
            allFiles.value = response.smartMap { responseValue ->
                val previewResponse = runBlocking { loadPreviews(responseValue.filter { it.type == FileType.FILE }, scope, apiContext) }
                val files = responseValue.map { file -> FileCacheElement(file, previewResponse.valueOrNull()?.firstOrNull { it.file.id == file.id }?.previewUrl) }

                // insert into live data
                val value = allFiles.value
                if (value != null) {
                    allFiles.value?.valueOrNull()?.toMutableList()?.apply {
                        if (overwriteExisting) {
                            removeAll { it.file.parentId == parentId }
                        }
                        addAll(files)
                    }?.distinctBy { file -> file.file.id } ?: emptyList()
                } else {
                    files
                }
            }
        }
    }

    fun loadChildrenTree(scope: IOperatingScope, idTree: String, overwriteExisting: Boolean, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = fileStorageRepository.getFileTree(idTree, true, scope, apiContext)
            val allFiles = getAllFiles(scope) as MutableLiveData
            allFiles.value = response.smartMap { responseValue ->
                val previewResponse = runBlocking { loadPreviews(responseValue.filter { it.type == FileType.FILE }, scope, apiContext) }
                val files = responseValue.map { file -> FileCacheElement(file, previewResponse.valueOrNull()?.firstOrNull { it.file.id == file.id }?.previewUrl) }

                // insert into live data
                val value = allFiles.value
                if (value != null) {
                    return@smartMap allFiles.value?.valueOrNull()?.toMutableList()?.apply {
                        if (overwriteExisting) {
                            removeAll { idTree.startsWith(it.file.id) }
                        }
                        addAll(files)
                    }?.distinctBy { file -> file.file.id } ?: emptyList()
                } else {
                    return@smartMap files
                }
            }
        }
    }

    fun loadChildrenNameTree(scope: IOperatingScope, nameTree: String, overwriteExisting: Boolean, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = fileStorageRepository.getFileNameTree(nameTree, true, scope, apiContext)
            val response = Response.Success(responses.map { it.valueOrNull() ?: emptyList() }.flatten())
            val allFiles = getAllFiles(scope) as MutableLiveData
            allFiles.value = response.smartMap { responseValue ->
                val previewResponse = runBlocking { loadPreviews(responseValue.filter { it.type == FileType.FILE }, scope, apiContext) }
                val files = responseValue.map { file -> FileCacheElement(file, previewResponse.valueOrNull()?.firstOrNull { it.file.id == file.id }?.previewUrl) }.distinctBy { it.file.id }

                // insert into live data
                val value = allFiles.value
                if (value != null) {
                    return@smartMap allFiles.value?.valueOrNull()?.toMutableList()?.apply {
                        if (overwriteExisting) {
                            removeAll { remove -> files.any { it.file.id == remove.file.id } }
                        }
                        addAll(files)
                    }?.distinctBy { file -> file.file.id } ?: emptyList()
                } else {
                    return@smartMap files
                }
            }
        }
    }

    private suspend fun loadPreviews(files: List<IRemoteFile>, scope: IOperatingScope, apiContext: IApiContext): Response<List<FileCacheElement>> {
        return fileStorageRepository.getFilePreviews(files, scope, apiContext).smartMap { value ->
            value.map { preview -> FileCacheElement(files.first { it.id == preview.key }, preview.value) }
        }
    }

    fun getCachedChildren(scope: IOperatingScope, parentId: String): List<FileCacheElement> {
        val files = getAllFiles(scope).value?.valueOrNull() ?: emptyList()
        return files.filter { it.file.parentId == parentId }
    }

    fun addFolder(name: String, parent: IRemoteFile, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = fileStorageRepository.addFolder(name, "", parent, scope, apiContext)
            if (response is Response.Success) {
                val allFiles = getAllFiles(scope) as MutableLiveData
                allFiles.value = allFiles.value?.smartMap { files ->
                    files.toMutableList().apply {
                        add(FileCacheElement(response.value))
                    }
                }
            }
            _addFolderResponse.value = response
        }
    }

    fun startOpenDownload(workManager: WorkManager, apiContext: IApiContext, file: IRemoteFile, scope: IOperatingScope, destinationUrl: String) {
        viewModelScope.launch {
            val response = fileStorageRepository.getFileDownloadUrl(file, scope, apiContext)
            if (response is Response.Success) {
                val workRequest = DownloadOpenWorker.createRequest(destinationUrl, response.value.url, file.name, file.size)
                addNetworkTransfer(NetworkTransfer.DownloadOpen(workRequest.id, file.id))
                workManager.enqueue(workRequest)
            }
        }
    }

    fun startSaveDownload(workManager: WorkManager, apiContext: IApiContext, file: IRemoteFile, scope: IOperatingScope, destinationUrl: String) {
        viewModelScope.launch {
            val response = fileStorageRepository.getFileDownloadUrl(file, scope, apiContext)
            if (response is Response.Success) {
                val workRequest = DownloadSaveWorker.createRequest(destinationUrl, response.value.url, file)
                addNetworkTransfer(NetworkTransfer.DownloadSave(workRequest.id, file.id))
                workManager.enqueue(workRequest)
            }
        }
    }

    fun startUpload(workManager: WorkManager, scope: IOperatingScope, apiContext: IApiContext, uri: Uri, fileName: String, size: Long, parentId: String) {
        // inject placeholder
        val id = "upload_transfer_${networkTransfers.value?.size ?: 0}"
        val liveData = getAllFiles(scope) as MutableLiveData
        liveData.value = liveData.value?.smartMap {
            it.toMutableList().apply {
                val userScope = RemoteScope(apiContext.user.login, apiContext.user.name, apiContext.user.type, null, true)
                add(0, FileCacheElement(RemoteFilePlaceholder(id, fileName, size, parentId, Modification(userScope, Date()))))
            }
        }

        // start upload
        val workRequest = SessionFileUploadWorker.createRequest(uri, fileName, apiContext.userContext())
        addNetworkTransfer(NetworkTransfer.Upload(workRequest.id, id))
        workManager.enqueue(workRequest)
    }

    fun importSessionFile(sessionFile: ISessionFile, into: IRemoteFileProvider?, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = fileStorageRepository.importSessionFile(sessionFile, into, scope, apiContext)
            if (response is Response.Success) {
                val allFiles = getAllFiles(scope) as MutableLiveData
                allFiles.value = response.smartMap { responseValue ->
                    val previewResponse = runBlocking { loadPreviews(listOf(response.value), scope, apiContext) }
                    val files = listOf(FileCacheElement(responseValue, previewResponse.valueOrNull()?.firstOrNull { it.file.id == responseValue.id }?.previewUrl))

                    // insert into live data
                    val value = allFiles.value
                    if (value != null) {
                        allFiles.value?.valueOrNull()?.toMutableList()?.apply {
                            addAll(files)
                        }?.distinctBy { file -> file.file.id } ?: emptyList()
                    } else {
                        files
                    }
                }
            }
            _importSessionFileResponse.value = response
        }
    }

    fun editFile(file: IRemoteFile, name: String, description: String?, downloadNotificationMe: Boolean?, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = fileStorageRepository.editFile(file, name, description, downloadNotificationMe, scope, apiContext)
            _editFileResponse.value = response
            if (response is Response.Success) {
                val allFiles = getAllFiles(scope) as MutableLiveData
                allFiles.value = allFiles.value?.smartMap { files ->
                    files.toMutableList().apply {
                        val orig = first { it.file.id == file.id }
                        set(indexOf(orig), FileCacheElement(file, orig.previewUrl))
                    }
                }
            }
        }
    }

    fun editFolder(file: IRemoteFile, name: String, description: String?, readable: Boolean?, writable: Boolean?, uploadNotificationMe: Boolean?, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = fileStorageRepository.editFolder(file, name, description, readable, writable, uploadNotificationMe, scope, apiContext)
            _editFileResponse.value = response
            if (response is Response.Success) {
                val allFiles = getAllFiles(scope) as MutableLiveData
                allFiles.value = allFiles.value?.smartMap { files ->
                    files.toMutableList().apply {
                        val orig = first { it.file.id == file.id }
                        set(indexOf(orig), FileCacheElement(file, orig.previewUrl))
                    }
                }
            }
        }
    }

    fun resetImportSessionFileResponse() {
        _importSessionFileResponse.value = null
    }

    private fun addNetworkTransfer(networkTransfer: NetworkTransfer) {
        val transfers = (_networkTransfers.value ?: emptyList()).toMutableList()
        transfers.add(networkTransfer)
        _networkTransfers.value = transfers
    }

    fun hideNetworkTransfer(networkTransfer: NetworkTransfer, scope: IOperatingScope) {
        if (networkTransfer is NetworkTransfer.Upload) {
            val liveData = getAllFiles(scope) as MutableLiveData
            liveData.value = liveData.value?.smartMap {
                it.toMutableList().apply {
                    removeAll { file -> file.file.id == networkTransfer.id }
                }
            }
        }

        val transfers = (_networkTransfers.value ?: emptyList()).toMutableList()
        transfers.remove(networkTransfer)
        _networkTransfers.value = transfers
    }

    fun batchDelete(files: List<IRemoteFile>, scope: IOperatingScope, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = files.map { fileStorageRepository.deleteFile(it, scope, apiContext) }
            val filesLiveData = getAllFiles(scope) as MutableLiveData
            val filesResponse = filesLiveData.value?.valueOrNull()
            if (filesResponse != null) {
                val currentFiles = filesResponse.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentFiles.removeAll { it.file.id == response.value.id }
                    }
                }
                filesLiveData.value = Response.Success(currentFiles)
            }
            _batchDeleteResponse.value = responses
        }
    }

    fun resetBatchDeleteResponse() {
        _batchDeleteResponse.value = null
    }

    fun resetAddFolderResponse() {
        _addFolderResponse.value = null
    }

    fun resetEditFileResponse() {
        _editFileResponse.value = null
    }

    fun cleanCache(scope: IOperatingScope) {
        _files[scope]?.value = Response.Success(emptyList())
    }

    override fun resetScopedData() {
        _files.forEach { (_, response) ->
            response.value = null
        }
        _addFolderResponse.value = null
        _batchDeleteResponse.value = null
        _importSessionFileResponse.value = null
        _quotas.value = null
        _networkTransfers.value = emptyList()

        quotaFilter.value = FileStorageQuotaFilter()
        fileFilter.value = FileStorageFileFilter()
    }

    fun resolveNameTree(scope: IOperatingScope, nameTree: String): String? {
        var lastId = "/"
        val files = _files[scope]?.value?.valueOrNull() ?: return null
        nameTree.split("/").forEach { child ->
            if (child.isNotEmpty()) {
                lastId = files.firstOrNull { it.file.parentId == lastId && it.file.name == child }?.file?.id ?: return null
            }
        }
        return lastId
    }
}