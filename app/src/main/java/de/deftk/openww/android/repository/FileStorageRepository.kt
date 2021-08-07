package de.deftk.openww.android.repository

import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.implementation.feature.filestorage.RemoteFile
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.model.feature.filestorage.IRemoteFileProvider
import de.deftk.openww.api.model.feature.filestorage.session.ISessionFile
import de.deftk.openww.api.request.OperatingScopeApiRequest
import de.deftk.openww.api.request.UserApiRequest
import de.deftk.openww.api.response.ResponseUtil
import kotlinx.serialization.json.*
import javax.inject.Inject

class FileStorageRepository @Inject constructor() : AbstractRepository() {

    suspend fun getAllFileStorageQuotas(apiContext: IApiContext) = apiCall {
        apiContext.user.getAllFileStorageQuotas(apiContext)
    }

    suspend fun getProviderFiles(provider: IRemoteFileProvider, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        provider.getFiles(context = scope.getRequestContext(apiContext))
    }

    suspend fun getFiles(parentId: String, getSelf: Boolean, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        val request = OperatingScopeApiRequest(scope.getRequestContext(apiContext))
        val id = request.addGetFileStorageFilesRequest(
            folderId = parentId,
            getFiles = true,
            getFolders = true,
            getFolder = getSelf,
            recursive = false
        )[1]
        val response = request.fireRequest()
        val subResponse = ResponseUtil.getSubResponseResult(response.toJson(), id)
        subResponse["entries"]!!.jsonArray.map { WebWeaverClient.json.decodeFromJsonElement<RemoteFile>(it) }
    }

    suspend fun getFileDownloadUrl(file: IRemoteFile, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        file.getDownloadUrl(scope.getRequestContext(apiContext))
    }

    suspend fun getFilePreviews(files: List<IRemoteFile>, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        scope.getFilePreviews(files, apiContext)
    }

    suspend fun importSessionFile(sessionFile: ISessionFile, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        scope.importSessionFile(sessionFile, context = scope.getRequestContext(apiContext))
    }

    suspend fun deleteFile(file: IRemoteFile, scope: IOperatingScope, apiContext: IApiContext) = apiCall {
        file.delete(scope.getRequestContext(apiContext))
        file
    }

    // extend json api to allow more efficient requests

    private fun UserApiRequest.addGetAllFileStorageQuotasRequest(user: IUser): List<Int> {
        val ids = mutableListOf<Int>()
        if (Feature.FILES.isAvailable(user.effectiveRights))
            ids.addAll(addGetFileStorageStateRequest())
        user.getGroups().filter { Feature.FILES.isAvailable(it.effectiveRights) }.forEach { group ->
            ids.addAll(addGetFileStorageStateRequest(group.login))
        }
        return ids
    }

    private suspend fun IUser.getAllFileStorageQuotas(apiContext: IApiContext): Map<IOperatingScope, Quota> {
        val request = UserApiRequest(getRequestContext(apiContext))
        val requestIds = request.addGetAllFileStorageQuotasRequest(apiContext.user)
        val response = request.fireRequest().toJson().jsonArray
        val quotas = mutableMapOf<IOperatingScope, Quota>()
        val responses = response.filter { requestIds.contains(it.jsonObject["id"]!!.jsonPrimitive.int) }.map { it.jsonObject }
        responses.withIndex().forEach { (index, subResponse) ->
            if (index % 2 == 1) {
                val focus = responses[index - 1]["result"]!!.jsonObject
                check(focus["method"]?.jsonPrimitive?.content == "set_focus")
                val memberLogin = focus["user"]!!.jsonObject["login"]!!.jsonPrimitive.content
                val scope = apiContext.findOperatingScope(memberLogin)!!
                quotas[scope] = WebWeaverClient.json.decodeFromJsonElement(subResponse["result"]!!.jsonObject["quota"]!!.jsonObject)
            }
        }
        return quotas
    }

    private suspend fun IOperatingScope.getFilePreviews(files: List<IRemoteFile>, apiContext: IApiContext): Map<String, FilePreviewUrl?> {
        val request = OperatingScopeApiRequest(getRequestContext(apiContext))

        val requestIds = mutableListOf<Int>()
        val idMap = mutableMapOf<Int, String>()
        files.forEach { file ->
            if (file.preview == true) {
                val req = request.addGetPreviewDownloadUrlRequest(file.id)
                check(req.size == 2)
                requestIds.addAll(req)
                idMap[req[1]] = file.id
            }
        }

        val response = request.fireRequest().toJson().jsonArray
        val previews = mutableMapOf<String, FilePreviewUrl?>()
        val responses = response.filter { requestIds.contains(it.jsonObject["id"]!!.jsonPrimitive.int) }.map { it.jsonObject }
        responses.forEach { subResponse ->
            val resp = subResponse["result"]!!.jsonObject
            if (resp["method"]?.jsonPrimitive?.content == "get_preview_download_url") {
                val previewUrl: FilePreviewUrl = WebWeaverClient.json.decodeFromJsonElement(resp["file"]!!.jsonObject)
                val id = subResponse["id"]!!.jsonPrimitive.int
                previews[idMap[id]!!] = previewUrl
            }
        }
        return previews
    }

}