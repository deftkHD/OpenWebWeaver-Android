package de.deftk.openww.android.viewmodel

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.feature.devtools.PastRequest
import de.deftk.openww.android.repository.login.LoginRepository
import de.deftk.openww.android.utils.DebugUtil
import de.deftk.openww.api.auth.Credentials
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IRequestContext
import de.deftk.openww.api.request.ApiRequest
import de.deftk.openww.api.request.handler.AutoLoginRequestHandler
import de.deftk.openww.api.request.handler.IRequestHandler
import de.deftk.openww.api.response.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val application: Application, private val loginRepository: LoginRepository): ScopedViewModel(savedStateHandle) {

    private val _loginResponse = registerProperty<Response<IApiContext>?>("loginResponse", true)
    val loginResponse: LiveData<Response<IApiContext>?> = _loginResponse
    val apiContext: LiveData<IApiContext?> = loginRepository.apiContext.asLiveData()

    private val _loginToken = registerProperty<Response<Pair<String, String>>>("loginToken", false)
    val loginToken: LiveData<Response<Pair<String, String>>> = _loginToken

    private val _logoutResponse = registerProperty<Response<Unit>?>("logoutResponse", true)
    val logoutResponse: LiveData<Response<Unit>?> = _logoutResponse

    private val _pastRequests = registerProperty<MutableList<PastRequest>>("pastRequests", false)
    val pastRequests: LiveData<MutableList<PastRequest>> = _pastRequests
    private var nextRequestId = 0

    fun loginPassword(username: String, password: String) {
        viewModelScope.launch {
            /*val resource = loginRepository.loginPassword(username, password)
            if (resource is Response.Success) {
                setupApiContext(resource.value, Credentials.fromPassword(username, password))
            }
            _loginResponse.postValue(resource)*/
        }
    }

    fun loginPasswordCreateToken(username: String, password: String) {
        viewModelScope.launch {
            /*val response = loginRepository.loginPasswordCreateToken(username, password)
            if (response is Response.Success) {
                setupApiContext(response.value.first, Credentials.fromToken(username, response.value.second))
            }
            _loginToken.postValue(response.smartMap { it.first.user.login to it.second })
            _loginResponse.postValue(response.smartMap { it.first })*/
        }
    }

    fun loginToken(username: String, token: String) {
        viewModelScope.launch {
            loginRepository.loginToken(username, token)


            /*_loginResponse.postValue(null)
            val resource = loginRepository.loginTokenOld(username, token)
            if (resource is Response.Success) {
                setupApiContext(resource.value, Credentials.fromToken(username, token))
            }
            _loginToken.postValue(resource.smartMap { username to token })
            _loginResponse.postValue(resource)*/
        }
    }

    fun loginAccount(account: Account, token: String) {
        return loginToken(account.name, token)
    }

    fun logout(login: String, context: Context) {
        /*val accountManager = AccountManager.get(context)
        val account = AuthHelper.findAccounts(login, context).firstOrNull()
        if (account == null) {
            viewModelScope.launch {
                apiContext.value?.also { apiContext ->
                    val response = loginRepository.logout(apiContext)
                    _logoutResponse.postValue(response)
                    _loginResponse.postValue(null)
                }
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            accountManager.removeAccount(account, null, { future ->
                if (future.isDone) {
                    val bundle = future.result
                    when {
                        bundle.containsKey(AccountManager.KEY_ERROR_MESSAGE) -> _logoutResponse.postValue(Response.Failure(IllegalStateException(bundle.getString(AccountManager.KEY_ERROR_MESSAGE))))
                        bundle.containsKey(AccountManager.KEY_ERROR_CODE) -> _logoutResponse.postValue(Response.Failure(IllegalStateException("Code: " + bundle.getInt(AccountManager.KEY_ERROR_CODE))))
                        bundle.containsKey(AccountManager.KEY_BOOLEAN_RESULT) -> _logoutResponse.postValue(if (bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) Response.Success(Unit) else Response.Failure(IllegalStateException()))
                        else -> _logoutResponse.postValue(Response.Failure(IllegalArgumentException()))
                    }
                    if (_logoutResponse.value is Response.Success) {
                        _loginResponse.postValue(null)
                    }
                }
            }, null)
        } else {
            @Suppress("DEPRECATION")
            accountManager.removeAccount(account, { future ->
                if (future.isDone) {
                    _logoutResponse.postValue(if (future.result) Response.Success(Unit) else Response.Failure(IllegalStateException()))
                    if (_logoutResponse.value is Response.Success) {
                        _loginResponse.postValue(null)
                    }
                }
            }, null)
        }*/
    }

    private fun setupApiContext(apiContext: IApiContext, credentials: Credentials) {
        val delegate = AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override suspend fun getCredentials(): Credentials = credentials

            override suspend fun onLogin(context: ApiContext) {
                withContext(Dispatchers.Main) {
                    _loginResponse.postValue(Response.Success(context))
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
        if (DebugUtil.areDevToolsEnabled(application)) {
            val list = _pastRequests.value ?: mutableListOf()
            list.add(PastRequest(nextRequestId++, request, response, Date()))
            withContext(Dispatchers.Main) {
                _pastRequests.value = list
            }
        }
    }

}