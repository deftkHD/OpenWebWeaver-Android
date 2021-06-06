package de.deftk.openlonet.viewmodel

import androidx.lifecycle.*
import de.deftk.lonet.api.LoNetClient
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.api.model.IOperatingScope
import de.deftk.lonet.api.model.IUser
import de.deftk.lonet.api.model.feature.Quota
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFile
import de.deftk.lonet.api.request.UserApiRequest
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.feature.filestorage.FileCacheElement
import kotlinx.serialization.json.*

class FileStorageViewModel : ViewModel() {

    /*private val _quotas = mutableMapOf<IOperatingScope, MutableLiveData<Quota>>()

    private val _quotaSummary = MutableLiveData<List<Pair<IOperatingScope, Quota>>>(null)
    val quotaSummaryResponse: LiveData<List<Pair<IOperatingScope, Quota>>> = _quotaSummary

    private val _files = mutableMapOf<IOperatingScope, MutableLiveData<List<FileCacheElement>>>()

    private val _previews = mutableMapOf<IOperatingScope, MutableMap<IRemoteFile, MutableLiveData<String>>>()

    fun getQuota(scope: IOperatingScope?): LiveData<Quota> {
        if (scope == null)
            return MutableLiveData(Quota(-1, -1, -1, -1, -1, -1))
        return _quotas.getOrPut(scope) { MutableLiveData() }
    }

    fun refreshQuota(scope: IOperatingScope, apiContext: ApiContext): LiveData<Response<Quota>> {
        return liveData {
            /*emit(Response<Quota>(ApiState.LOADING, null))
            try {
                val quota = withContext(Dispatchers.IO) {
                    scope.getFileStorageState(context = scope.getRequestContext(apiContext)).second
                }
                _quotas.getOrPut(scope) { MutableLiveData() }.value = quota
                val list = _quotaSummary.value?.toMutableList() ?: mutableListOf()
                if (list.any { it.first == scope }) {
                    list.remove(list.first { it.first == scope })
                }
                list.add(Pair(scope, quota))
                _quotaSummary.value = list

                emit(Response(ApiState.SUCCESS, quota))
            } catch (e: Exception) {
                emit(Response<Quota>(ApiState.ERROR, null, e))
            }*/
        }
    }

    fun refreshQuotaSummary(apiContext: ApiContext): LiveData<Response<Unit>> {
        return liveData {
            /*emit(Response<Unit>(ApiState.LOADING, null))
            try {
                val quotas = withContext(Dispatchers.IO) {
                    apiContext.getUser().getAllFileStorageQuotas(apiContext)
                }.toList()
                _quotaSummary.value = quotas
                quotas.forEach { (scope, quota) ->
                    _quotas[scope]?.value = quota
                }
                emit(Response<Unit>(ApiState.SUCCESS, null))
            } catch (e: Exception) {
                emit(Response<Unit>(ApiState.ERROR, null, e))
            }*/
        }
    }

    fun getRootFiles(operator: IOperatingScope?): LiveData<List<FileCacheElement>> {
        if (operator == null)
            return MutableLiveData(emptyList())
        return _files.getOrPut(operator) { MutableLiveData() }
    }

    fun refreshRootFiles(scope: IOperatingScope, apiContext: ApiContext): LiveData<Response<List<FileCacheElement>>> {
        return liveData {
            /*emit(Response<List<FileCacheElement>>(ApiState.LOADING, null))
            try {
                val files = withContext(Dispatchers.IO) {
                    scope.getFiles(context = scope.getRequestContext(apiContext))
                }.map { FileCacheElement(it, null) }
                _files.getOrPut(scope) { MutableLiveData() }.value = files
                emit(Response(ApiState.SUCCESS, files))
            } catch (e: Exception) {
                emit(Response<List<FileCacheElement>>(ApiState.ERROR, null, e))
            }*/
        }
    }

    fun getFolderDataHolder(operator: IOperatingScope, path: Array<String>?, folderId: String?): LiveData<List<IRemoteFile>> {
        return if (folderId == null) {
            getRootFiles(operator).map { it.map { it.file } }
        } else {
            Transformations.map(_files[operator]!!) {
                getFolderLiveDataFromCache(operator, path, folderId)?.children?.map { it.file }
            }
        }
    }

    private fun getFolderLiveDataFromCache(operator: IOperatingScope?, path: Array<String>?, id: String): FileCacheElement? {
        val rootFiles = _files[operator]?.value ?: return null
        return findFolderLiveData(rootFiles, path?.toMutableList(), id)
    }

    fun findFolderLiveData(rootFiles: List<FileCacheElement>, path: MutableList<String>?, id: String): FileCacheElement? {
        var files = rootFiles

        while (true) {
            if (path == null || path.isEmpty()) {
                return files.firstOrNull { it.file.id == id }
            } else {
                val root = files.firstOrNull { it.file.id == path[0] }
                if (root != null) {
                    path.removeFirst()
                    files = root.children ?: emptyList()
                    // repeat loop
                } else {
                    // path segment not found
                    return null
                }
            }
        }
    }

    // hopefully this never breaks
    fun cacheDirectory(scope: IOperatingScope, path: MutableList<String>?, directoryId: String, apiContext: ApiContext): LiveData<Response<FileCacheElement>> {
        return liveData {
            /*emit(Response<FileCacheElement>(ApiState.LOADING, null))
            try {
                val requestContext = scope.getRequestContext(apiContext)
                val rootFiles =_files[scope]?.value ?: withContext(Dispatchers.IO) {
                    scope.getFiles(context = requestContext).map { FileCacheElement(it, null) }
                }
                var files = rootFiles

                while (true) {
                    if (path == null || path.isEmpty()) {
                        val targetDirectory = files.firstOrNull { it.file.id == directoryId }
                        if (targetDirectory != null) {
                            if (targetDirectory.children == null) {
                                targetDirectory.children = withContext(Dispatchers.IO) {
                                    targetDirectory.file.getFiles(context = requestContext).map { FileCacheElement(it, null) }
                                }
                            }
                            _files.getOrPut(scope) { MutableLiveData() }.value = rootFiles
                            emit(Response(ApiState.SUCCESS, targetDirectory))
                            return@liveData
                        } else {
                            // directory not found
                            emit(Response<FileCacheElement>(ApiState.ERROR, null, IllegalStateException("directory $directoryId not found")))
                            return@liveData
                        }
                    } else {
                        val root = files.firstOrNull { it.file.id == path[0] }
                        if (root != null) {
                            path.removeFirst()
                            if (root.children == null) {
                                root.children = withContext(Dispatchers.IO) {
                                    root.file.getFiles(context = requestContext).map { FileCacheElement(it, null) }
                                }
                            }
                            files = root.children ?: emptyList()
                            // repeat loop
                        } else {
                            // path segment not found
                            emit(Response<FileCacheElement>(ApiState.ERROR, null, IllegalStateException("path segment ${path[0]} not found")))
                            return@liveData
                        }
                    }
                }
            } catch (e: Exception) {
                emit(Response<FileCacheElement>(ApiState.ERROR, null, e))
            }*/
        }
    }

    fun getFilePreview(operator: IOperatingScope, file: IRemoteFile): LiveData<String> {
        val map = _previews.getOrPut(operator) { mutableMapOf() }
        return map.getOrPut(file) { MutableLiveData() }
    }

    fun refreshFilePreview(scope: IOperatingScope, file: IRemoteFile, apiContext: ApiContext): LiveData<Response<String>> {
        return liveData {
            /*emit(Response<String>(ApiState.LOADING, null))
            try {
                val previewUrl = withContext(Dispatchers.IO) {
                    file.getPreviewUrl(scope.getRequestContext(apiContext))
                }.url
                _previews.getOrPut(scope) { mutableMapOf() }.getOrPut(file) { MutableLiveData() }.value = previewUrl
                emit(Response(ApiState.SUCCESS, previewUrl))
            } catch (e: Exception) {
                emit(Response<String>(ApiState.ERROR, null, e))
            }*/
        }
    }

    fun invalidateFilePreviews(scope: IOperatingScope) {
        if (_previews.containsKey(scope))
            _previews.remove(scope)
    }*/

}