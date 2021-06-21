package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.feature.filestorage.DownloadOpenWorker
import de.deftk.openww.android.feature.filestorage.DownloadSaveWorker
import de.deftk.openww.android.feature.filestorage.FileCacheElement
import de.deftk.openww.android.feature.filestorage.NetworkTransfer
import de.deftk.openww.android.repository.FileStorageRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileStorageViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val fileStorageRepository: FileStorageRepository) : ViewModel() {

    private val _quotas = MutableLiveData<Response<Map<IOperatingScope, Quota>>>()
    val quotasResponse: LiveData<Response<Map<IOperatingScope, Quota>>> = _quotas

    private val _files = mutableMapOf<IOperatingScope, MutableLiveData<Response<List<FileCacheElement>>>>()

    private val _networkTransfers = MutableLiveData<List<NetworkTransfer>>()
    val networkTransfers: LiveData<List<NetworkTransfer>> = _networkTransfers

    fun loadQuotas(apiContext: ApiContext) {
        viewModelScope.launch {
            _quotas.value = fileStorageRepository.getAllFileStorageQuotas(apiContext)
        }
    }

    fun loadFiles(scope: IOperatingScope, directoryId: String?, path: List<String>?, apiContext: ApiContext) {
        if (directoryId != null) {
            cacheDirectory(scope, path, directoryId, apiContext)
        } else {
            loadRootFiles(scope, apiContext)
        }
    }

    private fun loadRootFiles(scope: IOperatingScope, apiContext: ApiContext) {
        viewModelScope.launch {
            val files = fileStorageRepository.getFiles(scope, scope, apiContext).smartMap { list -> list.map { FileCacheElement(it) } }
            loadPreviews(files, scope, apiContext)
            _files.getOrPut(scope) { MutableLiveData() }.value = files
        }
    }

    private suspend fun loadPreviews(filesResponse: Response<List<FileCacheElement>>, scope: IOperatingScope, apiContext: ApiContext) {
        if (filesResponse is Response.Success) {
            val previews = fileStorageRepository.getFilePreviews(filesResponse.value.map { it.file }, scope, apiContext)
            if (previews is Response.Success) {
                filesResponse.value.forEach { file ->
                    if (previews.value.containsKey(file.file.id)) {
                        file.previewUrl = previews.value[file.file.id]
                    }
                }
            } else if (previews is Response.Failure) {
                //TODO handle error
                previews.exception.printStackTrace()
            }
        }
    }

    fun getProviderLiveData(scope: IOperatingScope, folderId: String?, path: List<String>?): LiveData<Response<List<FileCacheElement>>> {
        return if (folderId == null) {
            _files.getOrPut(scope) { MutableLiveData() }
        } else {
            val cacheElement = getLiveDataFromCache(scope, folderId, path)!!
            cacheElement.children
        }
    }

    fun getLiveDataFromCache(scope: IOperatingScope, id: String, path: List<String>?): FileCacheElement? {
        val rootFiles = _files[scope] ?: return null
        return findLiveData(rootFiles, path, id)
    }

    private fun findLiveData(rootFiles: MutableLiveData<Response<List<FileCacheElement>>>, path: List<String>?, id: String): FileCacheElement? {
        val pathSteps = path?.toMutableList()
        var files = rootFiles

        while (true) {
            if (pathSteps == null || pathSteps.isEmpty()) {
                return files.value?.valueOrNull()?.firstOrNull { it.file.id == id }
            } else {
                val root = files.value?.valueOrNull()?.firstOrNull { it.file.id == pathSteps[0] }
                if (root != null) {
                    pathSteps.removeFirst()
                    files = root.children
                    // repeat loop
                } else {
                    // path segment not found
                    return null
                }
            }
        }
    }

    // hopefully this never breaks
    private fun cacheDirectory(scope: IOperatingScope, path: List<String>?, directoryId: String, apiContext: ApiContext) {
        val pathSteps = path?.toMutableList()
        viewModelScope.launch {
            val cachedRootFiles = _files[scope]?.value
            val rootFiles = if (cachedRootFiles == null) {
                val response = fileStorageRepository.getFiles(scope, scope, apiContext).smartMap { list -> list.map { FileCacheElement(it) } }
                loadPreviews(response, scope, apiContext)
                _files.getOrPut(scope) { MutableLiveData() }.value = response
                response.valueOrNull() ?: return@launch
            } else {
                cachedRootFiles.valueOrNull() ?: return@launch
            }
            var files = rootFiles
            var lastHolder: MutableLiveData<Response<List<FileCacheElement>>>? = _files[scope]!!

            while (true) {
                if (pathSteps == null || pathSteps.isEmpty()) {
                    val targetDirectory = files.firstOrNull { it.file.id == directoryId }
                    if (targetDirectory != null) {
                        if (targetDirectory.children.value == null) {
                            val childrenResponse = fileStorageRepository.getFiles(targetDirectory.file, scope, apiContext).smartMap { list -> list.map { FileCacheElement(it) } }
                            loadPreviews(childrenResponse, scope, apiContext)
                            targetDirectory.children.value = childrenResponse
                            if (childrenResponse is Response.Failure) {
                                childrenResponse.exception.printStackTrace()
                                //TODO handle error
                                return@launch
                            }
                        }
                        lastHolder = targetDirectory.children
                        lastHolder.value = targetDirectory.children.value ?: Response.Failure(IllegalStateException("no children"))
                        return@launch
                    } else {
                        lastHolder?.value = Response.Failure(IllegalStateException("directory $directoryId not found"))
                        return@launch
                    }
                } else {
                    val root = files.firstOrNull { it.file.id == pathSteps[0] }
                    if (root != null) {
                        pathSteps.removeFirst()
                        if (root.children.value == null) {
                            val response = fileStorageRepository.getFiles(root.file, scope, apiContext).smartMap { list -> list.map { FileCacheElement(it) } }
                            loadPreviews(response, scope, apiContext)
                            root.children.value = response
                            lastHolder = root.children
                            if (response is Response.Failure) {
                                response.exception.printStackTrace()
                                //TODO handle error
                                return@launch
                            }
                        }
                        files = root.children.value?.valueOrNull() ?: emptyList()
                        // repeat loop
                    } else {
                        // path segment not found
                        lastHolder?.value = Response.Failure(IllegalStateException("path segment ${pathSteps[0]} not found"))
                        return@launch
                    }
                }
            }
        }
    }

    fun cleanCache(scope: IOperatingScope, folderId: String?, path: List<String>?) {
        if (folderId == null) {
            _files[scope]?.value = null
        } else {
            val data = getLiveDataFromCache(scope, folderId, path)
            data?.children?.value = null
        }
    }

    fun startOpenDownload(workManager: WorkManager, apiContext: ApiContext, file: IRemoteFile, scope: IOperatingScope, destinationUrl: String) {
        viewModelScope.launch {
            val response = fileStorageRepository.getFileDownloadUrl(file, scope, apiContext)
            if (response is Response.Success) {
                val workRequest = DownloadOpenWorker.createRequest(destinationUrl, response.value.url, file)
                addNetworkTransfer(NetworkTransfer.DownloadOpen(workRequest.id, file.id))
                workManager.enqueue(workRequest)
            }
        }
    }

    fun startSaveDownload(workManager: WorkManager, apiContext: ApiContext, file: IRemoteFile, scope: IOperatingScope, destinationUrl: String) {
        viewModelScope.launch {
            val response = fileStorageRepository.getFileDownloadUrl(file, scope, apiContext)
            if (response is Response.Success) {
                val workRequest = DownloadSaveWorker.createRequest(destinationUrl, response.value.url, file)
                addNetworkTransfer(NetworkTransfer.DownloadSave(workRequest.id, file.id))
                workManager.enqueue(workRequest)
            }
        }
    }

    private fun addNetworkTransfer(networkTransfer: NetworkTransfer) {
        val transfers = (_networkTransfers.value ?: emptyList()).toMutableList()
        transfers.add(networkTransfer)
        _networkTransfers.value = transfers
    }

    fun hideNetworkTransfer(networkTransfer: NetworkTransfer) {
        val transfers = (_networkTransfers.value ?: emptyList()).toMutableList()
        transfers.remove(networkTransfer)
        _networkTransfers.value = transfers
    }

}