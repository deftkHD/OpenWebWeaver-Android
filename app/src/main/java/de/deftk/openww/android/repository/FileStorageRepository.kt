package de.deftk.openww.android.repository

import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.Feature
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.model.feature.filestorage.IRemoteFileProvider
import de.deftk.openww.api.request.OperatingScopeApiRequest
import de.deftk.openww.api.request.UserApiRequest
import kotlinx.serialization.json.*
import javax.inject.Inject

class FileStorageRepository @Inject constructor() : AbstractRepository() {

    suspend fun getAllFileStorageQuotas(apiContext: ApiContext) = apiCall {
        apiContext.user.getAllFileStorageQuotas(apiContext).toList().sortedWith(compareBy ({ it.first !is IUser }, { it.first.name })).toMap()
    }

    suspend fun getFiles(provider: IRemoteFileProvider, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        provider.getFiles(context = scope.getRequestContext(apiContext)).sortedWith(compareBy( { -it.type.ordinal }, { it.name }))
    }

    suspend fun getFileDownloadUrl(file: IRemoteFile, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        file.getDownloadUrl(scope.getRequestContext(apiContext))
    }

    suspend fun getFilePreviews(files: List<IRemoteFile>, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        scope.getFilePreviews(files, apiContext)
    }

    suspend fun deleteFile(file: IRemoteFile, scope: IOperatingScope, apiContext: ApiContext) = apiCall {
        file.delete(scope.getRequestContext(apiContext))
        file
    }

    // extend LoNetApi to allow more efficient requests

    private fun UserApiRequest.addGetAllFileStorageQuotasRequest(user: IUser): List<Int> {
        val ids = mutableListOf<Int>()
        if (Feature.FILES.isAvailable(user.effectiveRights))
            ids.addAll(addGetFileStorageStateRequest())
        user.getGroups().filter { Feature.FILES.isAvailable(it.effectiveRights) }.forEach { group ->
            ids.addAll(addGetFileStorageStateRequest(group.login))
        }
        return ids
    }

    private suspend fun IUser.getAllFileStorageQuotas(apiContext: ApiContext): Map<IOperatingScope, Quota> {
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

    private suspend fun IOperatingScope.getFilePreviews(files: List<IRemoteFile>, apiContext: ApiContext): Map<String, FilePreviewUrl?> {
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