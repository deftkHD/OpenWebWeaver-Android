package de.deftk.openww.android.repository

import android.os.Build
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.model.IApiContext
import javax.inject.Inject

class LoginRepository @Inject constructor() : AbstractRepository() {

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

    suspend fun logout(apiContext: IApiContext) = apiCall {
        apiContext.user.logout(apiContext.user.getRequestContext(apiContext))
    }

    suspend fun logoutDestroyToken(token: String, apiContext: IApiContext) = apiCall {
        apiContext.user.logoutDestroyToken(
            token,
            apiContext.user.getRequestContext(apiContext)
        )
    }

}