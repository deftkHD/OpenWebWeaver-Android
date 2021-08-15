package de.deftk.openww.android.viewmodel

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.feature.AppFeature
import de.deftk.openww.android.feature.overview.AbstractOverviewElement
import de.deftk.openww.android.filter.SystemNotificationFilter
import de.deftk.openww.android.repository.UserRepository
import de.deftk.openww.api.auth.Credentials
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.api.request.handler.AutoLoginRequestHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val userRepository: UserRepository) : ScopedViewModel() {

    private val _loginResponse = MutableLiveData<Response<IApiContext>?>()
    val loginResponse: LiveData<Response<IApiContext>?> = _loginResponse
    val apiContext: LiveData<IApiContext?> = loginResponse.map { if (it is Response.Success) it.value else null }

    private val _loginToken = MutableLiveData<Response<Pair<String, String>>>()
    val loginToken: LiveData<Response<Pair<String, String>>> = _loginToken

    private val _logoutResponse = MutableLiveData<Response<Unit>?>()
    val logoutResponse: LiveData<Response<Unit>?> = _logoutResponse

    private val _overviewResponse = MutableLiveData<Response<List<AbstractOverviewElement>>?>()
    val overviewResponse: LiveData<Response<List<AbstractOverviewElement>>?> = _overviewResponse

    private val _systemNotificationsResponse = MutableLiveData<Response<List<ISystemNotification>>?>()
    val allSystemNotificationsResponse: LiveData<Response<List<ISystemNotification>>?> = _systemNotificationsResponse

    val systemNotificationFilter = MutableLiveData<SystemNotificationFilter>()

    val filteredSystemNotificationResponse: LiveData<Response<List<ISystemNotification>>?>
        get() = systemNotificationFilter.switchMap { filter ->
            when (filter) {
                null -> allSystemNotificationsResponse
                else -> allSystemNotificationsResponse.switchMap { response ->
                    val filtered = MutableLiveData<Response<List<ISystemNotification>>?>()
                    filtered.value = response?.smartMap { filter.apply(it) }
                    filtered
                }
            }
        }

    private val _systemNotificationDeleteResponse = MutableLiveData<Response<ISystemNotification>?>()
    val systemNotificationDeleteResponse: LiveData<Response<ISystemNotification>?> = _systemNotificationDeleteResponse

    private val _systemNotificationBatchDeleteResponse = MutableLiveData<List<Response<ISystemNotification>>?>()
    val systemNotificationBatchDeleteResponse: LiveData<List<Response<ISystemNotification>>?> = _systemNotificationBatchDeleteResponse

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
            _loginToken.value = response.smartMap { it.first.user.login to it.second }
            _loginResponse.value = response.smartMap { it.first }
        }
    }

    fun loginToken(username: String, token: String) {
        viewModelScope.launch {
            _loginResponse.value = null
            val resource = userRepository.loginToken(username, token)
            if (resource is Response.Success) {
                setupApiContext(resource.value, Credentials.fromToken(username, token))
            }
            _loginToken.value = resource.smartMap { username to token }
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

    private fun setupApiContext(apiContext: IApiContext, credentials: Credentials) {
        apiContext.requestHandler = AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override suspend fun getCredentials(): Credentials = credentials

            override suspend fun onLogin(context: ApiContext) {
                withContext(Dispatchers.Main) {
                    _loginResponse.value = Response.Success(context)
                }
            }
        }, ApiContext::class.java)
    }

    fun loadOverview(features: List<AppFeature>, apiContext: IApiContext) {
        viewModelScope.launch {
            val resource = userRepository.getOverviewElements(features, apiContext)
            _overviewResponse.value = resource
        }
    }

    fun loadSystemNotifications(apiContext: IApiContext) {
        viewModelScope.launch {
            val resource = userRepository.getSystemNotifications(apiContext)
            _systemNotificationsResponse.value = resource
        }
    }

    fun deleteSystemNotification(systemNotification: ISystemNotification, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = userRepository.deleteSystemNotification(systemNotification, apiContext)
            _systemNotificationDeleteResponse.value = response
            if (_systemNotificationsResponse.value is Response.Success && response is Response.Success) {
                val systemNotifications = (_systemNotificationsResponse.value as Response.Success<List<ISystemNotification>>).value.toMutableList()
                systemNotifications.remove(systemNotification)
                _systemNotificationsResponse.value = Response.Success(systemNotifications)
            }
        }
    }

    fun batchDeleteSystemNotifications(systemNotifications: List<ISystemNotification>, apiContext: IApiContext) {
        viewModelScope.launch {
            val responses = systemNotifications.map { userRepository.deleteSystemNotification(it, apiContext) }
            _systemNotificationBatchDeleteResponse.value = responses
            val notifications = allSystemNotificationsResponse.value?.valueOrNull()
            if (notifications != null) {
                val currentNotifications = notifications.toMutableList()
                responses.forEach { response ->
                    if (response is Response.Success) {
                        currentNotifications.remove(response.value)
                    }
                }
                _systemNotificationsResponse.value = Response.Success(currentNotifications)
            }
        }
    }

    fun resetDeleteResponse() {
        _systemNotificationDeleteResponse.value = null
    }

    fun resetBatchDeleteResponse() {
        _systemNotificationBatchDeleteResponse.value = null
    }

    override fun resetScopedData() {
        _loginResponse.value = null
        _logoutResponse.value = null
        _systemNotificationsResponse.value = null
        _systemNotificationDeleteResponse.value = null
        _systemNotificationBatchDeleteResponse.value = null
        _overviewResponse.value = null
    }
}