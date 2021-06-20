package de.deftk.openlonet.viewmodel

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.lonet.api.auth.Credentials
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.feature.systemnotification.ISystemNotification
import de.deftk.lonet.api.request.handler.AutoLoginRequestHandler
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.auth.AuthHelper
import de.deftk.openlonet.feature.overview.AbstractOverviewElement
import de.deftk.openlonet.repository.UserRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val userRepository: UserRepository) : ViewModel() {

    private val _loginResponse = MutableLiveData<Response<ApiContext>?>()
    val loginResponse: LiveData<Response<ApiContext>?> = _loginResponse
    val apiContext: LiveData<ApiContext?> = loginResponse.map { if (it is Response.Success) it.value else null }

    private val _loginToken = MutableLiveData<Response<Pair<String, String>>>()
    val loginToken: LiveData<Response<Pair<String, String>>> = _loginToken

    private val _logoutResponse = MutableLiveData<Response<Unit>?>()
    val logoutResponse: LiveData<Response<Unit>?> = _logoutResponse

    private val _overviewResponse = MutableLiveData<Response<List<AbstractOverviewElement>>>()
    val overviewResponse: LiveData<Response<List<AbstractOverviewElement>>> = _overviewResponse

    private val _systemNotificationsResponse = MutableLiveData<Response<List<ISystemNotification>>>()
    val systemNotificationsResponse: LiveData<Response<List<ISystemNotification>>> = _systemNotificationsResponse

    fun loginPassword(username: String, password: String) {
        viewModelScope.launch {
            val resource = userRepository.loginPassword(username, password)
            if (resource is Response.Success) {
                setupApiContext(resource.value, Credentials.fromPassword(username, password))
            }
            _loginResponse.value = resource
        }
    }

    fun loginPasswordCreateToken(username: String, password: String) {
        viewModelScope.launch {
            val response = userRepository.loginPasswordCreateToken(username, password)
            if (response is Response.Success) {
                setupApiContext(response.value.first, Credentials.fromToken(username, response.value.second))
            }
            _loginToken.value = response.smartMap { it.first.getUser().login to it.second }
            _loginResponse.value = response.smartMap { it.first }
        }
    }

    fun loginToken(username: String, token: String) {
        viewModelScope.launch {
            val resource = userRepository.loginToken(username, token)
            if (resource is Response.Success) {
                setupApiContext(resource.value, Credentials.fromToken(username, token))
            }
            _loginResponse.value = resource
        }
    }

    fun loginAccount(account: Account, token: String) {
        return loginToken(account.name, token)
    }

    fun logout(login: String, context: Context) {
        val accountManager = AccountManager.get(context)
        val account = AuthHelper.findAccounts(login, context).firstOrNull()
        if (account == null) {
            viewModelScope.launch {
                apiContext.value?.also { apiContext ->
                    val response = userRepository.logout(apiContext)
                    _logoutResponse.value = response
                    _loginResponse.value = null
                }
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            accountManager.removeAccount(account, null, { future ->
                if (future.isDone) {
                    val bundle = future.result
                    when {
                        bundle.containsKey(AccountManager.KEY_ERROR_MESSAGE) -> _logoutResponse.value = Response.Failure(IllegalStateException(bundle.getString(AccountManager.KEY_ERROR_MESSAGE)))
                        bundle.containsKey(AccountManager.KEY_ERROR_CODE) -> _logoutResponse.value = Response.Failure(IllegalStateException("Code: " + bundle.getInt(AccountManager.KEY_ERROR_CODE)))
                        bundle.containsKey(AccountManager.KEY_BOOLEAN_RESULT) -> _logoutResponse.value = if (bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) Response.Success(Unit) else Response.Failure(IllegalStateException())
                        else -> _logoutResponse.value = Response.Failure(IllegalArgumentException())
                    }
                    if (_logoutResponse.value is Response.Success) {
                        _loginResponse.value = null
                    }
                }
            }, null)
        } else {
            @Suppress("DEPRECATION")
            accountManager.removeAccount(account, { future ->
                if (future.isDone) {
                    _logoutResponse.value = if (future.result) Response.Success(Unit) else Response.Failure(IllegalStateException())
                    if (_logoutResponse.value is Response.Success) {
                        _loginResponse.value = null
                    }
                }
            }, null)
        }
    }

    private fun setupApiContext(apiContext: ApiContext, credentials: Credentials) {
        apiContext.setRequestHandler(AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override fun getCredentials(): Credentials = credentials

            override fun onLogin(context: ApiContext) {
                _loginResponse.value = Response.Success(context)
            }
        }, ApiContext::class.java))
    }

    fun loadOverview(apiContext: ApiContext) {
        viewModelScope.launch {
            val resource = userRepository.getOverviewElements(apiContext)
            _overviewResponse.value = resource
        }
    }

    fun loadSystemNotifications(apiContext: ApiContext) {
        viewModelScope.launch {
            val resource = userRepository.getSystemNotifications(apiContext)
            _systemNotificationsResponse.value = resource
        }
    }

    fun deleteSystemNotification(systemNotification: ISystemNotification, apiContext: ApiContext): LiveData<Response<Unit>> {
        return liveData {
            val response = userRepository.deleteSystemNotification(systemNotification, apiContext)
            if (_systemNotificationsResponse.value is Response.Success && response is Response.Success) {
                val systemNotifications = (_systemNotificationsResponse.value as Response.Success<List<ISystemNotification>>).value.toMutableList()
                systemNotifications.remove(systemNotification)
                _systemNotificationsResponse.value = Response.Success(systemNotifications)
            }
            //TODO add _postResponse livedata
            emit(response)
        }
    }

}