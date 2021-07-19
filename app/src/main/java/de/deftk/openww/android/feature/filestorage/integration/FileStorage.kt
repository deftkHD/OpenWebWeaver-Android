package de.deftk.openww.android.feature.filestorage.integration

import android.util.Log
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.repository.FileStorageRepository
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.model.feature.filestorage.IRemoteFileProvider
import kotlin.IllegalStateException

class FileStorage {

    companion object {
        private val TAG = FileStorage::class.java.name
    }

    private val fileStorageRepository: FileStorageRepository = FileStorageRepository()
    private val files = mutableMapOf<IOperatingScope, Response<List<ProviderCacheElement>>>()

    fun getScopes(apiContext: IApiContext): List<IOperatingScope> {
        val scopes = mutableListOf<IOperatingScope>()
        if (Feature.FILES.isAvailable(apiContext.user.effectiveRights)) {
            scopes.add(apiContext.user)
        }
        scopes.addAll(apiContext.user.getGroups().filter { Feature.FILES.isAvailable(it.effectiveRights) })
        return scopes
    }

    suspend fun loadFiles(scope: IOperatingScope, directoryId: String?, path: List<String>?, apiContext: IApiContext) {
        if (directoryId == null) {
            loadRootFiles(scope, apiContext)
        } else {
            cacheDirectory(scope, directoryId, path, apiContext)
        }
    }

    suspend fun loadRootFiles(scope: IOperatingScope, apiContext: IApiContext): Response<List<ProviderCacheElement>> {
        val response = fileStorageRepository.getFiles(scope, scope, apiContext).smartMap { list -> list.map { ProviderCacheElement(scope, it) } }
        files[scope] = response
        return response
    }

    suspend fun cacheDirectory(scope: IOperatingScope, directoryId: String, path: List<String>?, apiContext: IApiContext): Response<List<ProviderCacheElement>> {
        val pathSteps = path?.toMutableList()
        val cachedRootFiles = files[scope]
        val rootFiles = if (cachedRootFiles == null) {
            val response = loadRootFiles(scope, apiContext)
            response.valueOrNull() ?: return response
        } else{
            cachedRootFiles.valueOrNull() ?: return Response.Failure(IllegalStateException("No root files cached"))
        }

        var files = rootFiles
        var lastResponse: Response<List<ProviderCacheElement>>? = this.files[scope]

        while (true) {
            if (pathSteps == null || pathSteps.isEmpty()) {
                val targetDirectory = files.firstOrNull { getProviderId(it.provider) == directoryId }
                if (targetDirectory != null) {
                    val response = fileStorageRepository.getFiles(targetDirectory.provider, scope, apiContext).smartMap { list -> list.map { ProviderCacheElement(scope, it) } }
                    targetDirectory.children = response
                    lastResponse = response
                    if (response is Response.Failure) {
                        Log.e(TAG, "Failed to query children of directory ${targetDirectory.provider.name}")
                        response.exception.printStackTrace()
                    }
                    break
                } else {
                    Log.e(TAG, "Directory $directoryId not found")
                    break
                }
            } else {
                val parent = files.firstOrNull { getProviderId(it.provider) == pathSteps[0] }
                if (parent != null) {
                    pathSteps.removeFirst()
                    if (parent.children == null) {
                        val response = fileStorageRepository.getFiles(parent.provider, scope, apiContext).smartMap { list -> list.map { ProviderCacheElement(scope, it) } }
                        parent.children = response
                        lastResponse = response
                        if (response is Response.Failure) {
                            Log.e(TAG, "Failed to query children of directory ${parent.provider.name}")
                            response.exception.printStackTrace()
                            break
                        }
                    }
                    files = parent.children?.valueOrNull() ?: emptyList()
                } else {
                    // path segment not found
                    Log.e(TAG, "Path segment ${pathSteps[0]} not found")
                    break
                }
            }
        }

        return lastResponse ?: Response.Failure(IllegalStateException())
    }
    
    fun getCachedChildren(scope: IOperatingScope, directoryId: String?, path: List<String>?): Response<List<ProviderCacheElement>>? {
        return if (directoryId == null) {
            files[scope]
        } else {
            return findCacheElement(scope, directoryId, path)?.children
        }
    }
    
    fun findCacheElement(scope: IOperatingScope, id: String, path: List<String>?): ProviderCacheElement? {
        val pathSteps = path?.toMutableList()
        var files = files[scope] ?: return null
        
        while (true) {
            if (pathSteps == null || pathSteps.isEmpty()) {
                return files.valueOrNull()?.firstOrNull { getProviderId(it.provider) == id }
            } else {
                val parent = files.valueOrNull()?.firstOrNull { getProviderId(it.provider) == pathSteps[0] }
                if (parent != null) {
                    pathSteps.removeFirst()
                    files = parent.children ?: return null
                    // repeat loop
                } else {
                    // path segment not found
                    Log.e(TAG, "Path segment ${pathSteps[0]} not found")
                    return null
                }
            }
        }
    }

    private fun getProviderId(provider: IRemoteFileProvider): String {
        return when (provider) {
            is IRemoteFile -> provider.id
            is IOperatingScope -> provider.name
            else -> error("Invalid or unknown provider")
        }
    }


}