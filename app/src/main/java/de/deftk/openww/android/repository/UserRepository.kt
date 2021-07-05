package de.deftk.openww.android.repository

import android.os.Build
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.api.request.UserApiRequest
import de.deftk.openww.api.response.ResponseUtil
import de.deftk.openww.android.feature.AppFeature
import de.deftk.openww.android.feature.overview.AbstractOverviewElement
import javax.inject.Inject

class UserRepository @Inject constructor() : AbstractRepository() {

    suspend fun loginPassword(username: String, password: String) = apiCall {
        WebWeaverClient.login(username, password)
    }

    suspend fun loginPasswordCreateToken(username: String, password: String) = apiCall {
        WebWeaverClient.loginCreateToken(
            username,
            password,
            "OpenWebWeaver",
            "${Build.BRAND} ${Build.MODEL}"
        )
    }

    suspend fun loginToken(username: String, token: String) = apiCall {
        WebWeaverClient.loginToken(username, token)
    }

    suspend fun logout(apiContext: ApiContext) = apiCall {
        apiContext.getUser().logout(apiContext.getUser().getRequestContext(apiContext))
    }

    suspend fun logoutDestroyToken(token: String, apiContext: ApiContext) = apiCall {
        apiContext.getUser().logoutDestroyToken(
            token,
            apiContext.getUser().getRequestContext(apiContext)
        )
    }

    suspend fun getOverviewElements(apiContext: ApiContext) = apiCall {
        val elements = mutableListOf<AbstractOverviewElement>()
        val request = UserApiRequest(apiContext.getUser().getRequestContext(apiContext))
        val idMap = mutableMapOf<AppFeature, List<Int>>()
        AppFeature.values().forEach { feature ->
            if (feature.overviewBuilder != null) {
                idMap[feature] = feature.overviewBuilder.appendRequests(request, apiContext.getUser())
            }
        }
        val response = request.fireRequest().toJson()
        idMap.forEach { (feature, ids) ->
            elements.add(feature.overviewBuilder!!.createElementFromResponse(ids.map { it to ResponseUtil.getSubResponseResult(response, it) }.toMap(), apiContext))
        }
        elements
    }

    suspend fun getSystemNotifications(apiContext: ApiContext) = apiCall {
        apiContext.getUser().getSystemNotifications(apiContext.getUser().getRequestContext(apiContext)).sortedByDescending { it.date.time }
    }

    suspend fun deleteSystemNotification(systemNotification: ISystemNotification, apiContext: ApiContext) = apiCall {
        systemNotification.delete(apiContext.getUser().getRequestContext(apiContext))
    }


}