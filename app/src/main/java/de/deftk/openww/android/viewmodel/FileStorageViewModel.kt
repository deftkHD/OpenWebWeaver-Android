package de.deftk.openww.android.viewmodel

import androidx.lifecycle.*
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.feature.filestorage.DownloadOpenWorker
import de.deftk.openww.android.feature.filestorage.DownloadSaveWorker
import de.deftk.openww.android.feature.filestorage.FileCacheElement
import de.deftk.openww.android.feature.filestorage.NetworkTransfer
import de.deftk.openww.android.filter.FileStorageFileFilter
import de.deftk.openww.android.filter.FileStorageQuotaFilter
import de.deftk.openww.android.repository.FileStorageRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class FileStorageViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val fileStorageRepository: FileStorageRepository) : ViewModel() {

    private val _quotas = MutableLiveData<Response<Map<IOperatingScope, Quota>>>()
    val allQuotasResponse: LiveData<Response<Map<IOperatingScope, Quota>>> = _quotas

    val quotaFilter = MutableLiveData(FileStorageQuotaFilter())
    val filteredQuotasResponse: LiveData<Response<Map<IOperatingScope, Quota>>>
        get() = quotaFilter.switchMap { filter ->
            when (filter) {
                null -> allQuotasResponse
                else -> allQuotasResponse.switchMap { response ->
                    val filtered = MutableLiveData<Response<Map<IOperatingScope, Quota>>>()
                    filtered.value = response.smartMap { filter.apply(it.toList()).toMap() }
                    filtered
                }
            }
        }
    private val _files = mutableMapOf<IOperatingScope, MutableLiveData<Response<List<FileCacheElement>>>>()
    val fileFilter = MutableLiveData(FileStorageFileFilter())
    private val _filteredFiles = mutableMapOf<IOperatingScope, LiveData<Response<List<FileCacheElement>>>>()

    private val _batchDeleteResponse = MutableLiveData<List<Response<IRemoteFile>>?>()
    val batchDeleteResponse: LiveData<List<Response<IRemoteFile>>?> = _batchDeleteResponse

    private val _networkTransfers = MutableLiveData<List<NetworkTransfer>>()
    val networkTransfers: LiveData<List<NetworkTransfer>> = _networkTransfers

    fun loadQuotas(apiContext: IApiContext) {
        viewModelScope.launch {
            _quotas.value = fileStorageRepository.getAllFileStorageQuotas(apiContext)
        }
    }

    fun getAllFiles(scope: IOperatingScope): LiveData<Response<List<FileCacheElement>>> {
        return _files.getOrPut(scope) { MutableLiveData() }
    }

    fun getFilteredFiles(scope: IOperatingScope): LiveData<Response<List<FileCacheElement>>> {
        return _filteredFiles.getOrPut(scope) {
            fileFilter.switchMap { filter ->
                when (filter) {
                    null -> getAllFiles(scope)
                    else -> getAllFiles(scope).switchMap { response ->
                        val filtered = MutableLiveData<Response<List<FileCacheElement>>>()
                        filtered.value = response.smartMap { filter.apply(it.map { file -> file to scope }).map { pair -> pair.first } }
                        filtered
                    }
                }
            }
        }
    }

    fun loadChildren(scope: IOperatingScope, parentId: String, overwriteExisting: Boolean, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = fileStorageRepository.getFiles(parentId, parentId == "/" || parentId == "", scope, apiContext)
            val allFiles = getAllFiles(scope) as MutableLiveData
            allFiles.value = response.smartMap { responseValue ->
                val previewResponse = runBlocking { loadPreviews(responseValue.filter { it.type == FileType.FILE }, scope, apiContext) }
                val files = responseValue.map { file -> FileCacheElement(file, previewResponse.valueOrNull()?.firstOrNull { it.file.id == file.id }?.previewUrl) }

                // insert into live data
                val value = allFiles.value
                if (value != null && !overwriteExisting) {
                    allFiles.value?.valueOrNull()?.toMutableList()?.apply {
                        addAll(files)
                    }?.distinctBy { file -> file.file.id } ?: emptyList()
                } else {
                    files
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


}