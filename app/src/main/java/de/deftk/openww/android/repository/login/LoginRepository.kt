package de.deftk.openww.android.repository.login

import android.os.Build
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.feature.devtools.PastRequest
import de.deftk.openww.android.repository.AbstractRepository
import de.deftk.openww.android.utils.DebugUtil
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.auth.Credentials
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IRequestContext
import de.deftk.openww.api.request.ApiRequest
import de.deftk.openww.api.request.handler.AutoLoginRequestHandler
import de.deftk.openww.api.request.handler.IRequestHandler
import de.deftk.openww.api.response.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.*

class LoginRepository : AbstractRepository() {

    private val _apiContext = MutableStateFlow<IApiContext?>(null)
    val apiContext: Flow<IApiContext?>
        get() = _apiContext

    suspend fun loginPassword(username: String, password: String) {
        _apiContext.update {
            val apiContext = WebWeaverClient.login(username, password)
            setupApiContext(apiContext, Credentials.fromPassword(username, password))
            apiContext
        }
    }

    suspend fun loginPasswordCreateToken(username: String, password: String) {
        _apiContext.update {
            val (apiContext, token) = WebWeaverClient.loginCreateToken(
                username,
                password,
                "OpenWebWeaver",
                "${Build.BRAND} ${Build.MODEL}"
            )
            setupApiContext(apiContext, Credentials.fromToken(username, token))
            //TODO handle token
            apiContext
        }
    }

    suspend fun loginTokenOld(username: String, token: String) = apiCall {
        WebWeaverClient.loginToken(username, token)
    }

    suspend fun loginToken(username: String, token: String) {
        _apiContext.update {
            val apiContext = withContext(Dispatchers.IO) {
                WebWeaverClient.loginToken(username, token)
            }
            setupApiContext(apiContext, Credentials.fromToken(username, token))
            apiContext
        }
    }

    suspend fun logout(apiContext: IApiContext) {
        apiContext.user.logout(apiContext.user.getRequestContext(apiContext))
    }

    suspend fun logoutDestroyToken(token: String, apiContext: IApiContext) {
        apiContext.user.logoutDestroyToken(
            token,
            apiContext.user.getRequestContext(apiContext)
        )
    }

    private fun setupApiContext(apiContext: IApiContext, credentials: Credentials) {
        val delegate = AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override suspend fun getCredentials(): Credentials = credentials

            override suspend fun onLogin(context: ApiContext) {
                withContext(Dispatchers.Main) {
                    _apiContext.update {
                        context
                    }
                }
            }
        }, ApiContext::class.java)

        apiContext.requestHandler = object : IRequestHandler {
            override suspend fun performRequest(request: ApiRequest, context: IRequestContext): ApiResponse {
                val response = delegate.performRequest(request, context)
                handleNewApiResponse(request, response)
                return response
            }
        }
    }

    suspend fun handleNewApiResponse(request: ApiRequest, response: ApiResponse) {
        //TODO reimplement
        /*if (DebugUtil.areDevToolsEnabled(application)) {
            val list = _pastRequests.value ?: mutableListOf()
            list.add(PastRequest(nextRequestId++, request, response, Date()))
            withContext(Dispatchers.Main) {
                _pastRequests.value = list
            }
        }*/
    }

}