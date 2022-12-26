package de.deftk.openww.android.viewmodel

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.auth.AuthHelper
import de.deftk.openww.android.feature.AppFeature
import de.deftk.openww.android.feature.devtools.PastRequest
import de.deftk.openww.android.feature.overview.AbstractOverviewElement
import de.deftk.openww.android.filter.SystemNotificationFilter
import de.deftk.openww.android.repository.UserRepository
import de.deftk.openww.api.auth.Credentials
import de.deftk.openww.api.implementation.ApiContext
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.IRequestContext
import de.deftk.openww.api.model.feature.systemnotification.INotificationSetting
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.api.model.feature.systemnotification.NotificationFacility
import de.deftk.openww.api.model.feature.systemnotification.NotificationFacilityState
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
class UserViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val application: Application, private val userRepository: UserRepository) : ScopedViewModel(savedStateHandle) {

    private val _loginResponse = registerProperty<Response<IApiContext>?>("loginResponse", true)
    val loginResponse: LiveData<Response<IApiContext>?> = _loginResponse
    val apiContext: LiveData<IApiContext?> = loginResponse.map { if (it is Response.Success) it.value else null }

    private val _loginToken = registerProperty<Response<Pair<String, String>>>("loginToken", false)
    val loginToken: LiveData<Response<Pair<String, String>>> = _loginToken

    private val _logoutResponse = registerProperty<Response<Unit>?>("logoutResponse", true)
    val logoutResponse: LiveData<Response<Unit>?> = _logoutResponse

    private val _overviewResponse = registerProperty<Response<List<AbstractOverviewElement>>?>("overviewResponse", true)
    val overviewResponse: LiveData<Response<List<AbstractOverviewElement>>?> = _overviewResponse

    private val _systemNotificationsResponse = registerProperty<Response<List<ISystemNotification>>?>("systemNotificationsResponse", true)
    val allSystemNotificationsResponse: LiveData<Response<List<ISystemNotification>>?> = _systemNotificationsResponse

    val systemNotificationFilter = registerProperty<SystemNotificationFilter>("systemNotificationFilter", true)

    val _systemNotificationSettingsResponse = registerProperty<Response<List<INotificationSetting>>?>("systemNotificationSettingsResponse", true)
    val systemNotificationSettingsResponse: LiveData<Response<List<INotificationSetting>>?> = _systemNotificationSettingsResponse

    val filteredSystemNotificationResponse: LiveData<Response<List<ISystemNotification>>?>
        get() = systemNotificationFilter.switchMap { filter ->
            when (filter) {
                null -> allSystemNotificationsResponse
                else -> allSystemNotificationsResponse.switchMap { response ->
                    val filtered = registerProperty<Response<List<ISystemNotification>>?>("filtered", true)
                    filtered.value = response?.smartMap { filter.apply(it) }
                    filtered
                }
            }
        }

    private val _systemNotificationDeleteResponse = registerProperty<Response<ISystemNotification>?>("systemNotificationDeleteResponse", true)
    val systemNotificationDeleteResponse: LiveData<Response<ISystemNotification>?> = _systemNotificationDeleteResponse

    private val _systemNotificationBatchDeleteResponse = registerProperty<List<Response<ISystemNotification>>?>("systemNotificationBatchDeleteResponse", true)
    val systemNotificationBatchDeleteResponse: LiveData<List<Response<ISystemNotification>>?> = _systemNotificationBatchDeleteResponse

    private val _pastRequests = registerProperty<MutableList<PastRequest>>("pastRequests", false)
    val pastRequests: LiveData<MutableList<PastRequest>> = _pastRequests
    private var nextRequestId = 0

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
        val delegate = AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override suspend fun getCredentials(): Credentials = credentials

            override suspend fun onLogin(context: ApiContext) {
                withContext(Dispatchers.Main) {
                    _loginResponse.value = Response.Success(context)
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

    fun loadSystemNotificationSettings(apiContext: IApiContext) {
        viewModelScope.launch {
            val response = userRepository.getSystemNotificationSettings(apiContext)
            _systemNotificationSettingsResponse.value = response
        }
    }

    //FIXME seems like this doesn't edit anything serverside
    fun editSystemNotificationFacilities(setting: INotificationSetting, facility: NotificationFacilityState, apiContext: IApiContext) {
        viewModelScope.launch {
            val response = userRepository.editSystemNotificationSetting(setting, facility, apiContext)
            val settings = _systemNotificationSettingsResponse.value?.valueOrNull()
            if (settings != null && response is Response.Success) {
                val currentSettings = settings.toMutableList()
                val newSetting = response.value
                currentSettings.removeAll { it.type == setting.type }
                currentSettings.add(newSetting)
                _systemNotificationSettingsResponse.value = Response.Success(currentSettings)
            }
        }
    }

    suspend fun handleNewApiResponse(request: ApiRequest, response: ApiResponse) {
        if (PreferenceManager.getDefaultSharedPreferences(application).getBoolean("show_devtools", false)) {
            val list = _pastRequests.value ?: mutableListOf()
            list.add(PastRequest(nextRequestId++, request, response, Date()))
            withContext(Dispatchers.Main) {
                _pastRequests.value = list
            }
        }
    }

    fun enableAllSystemNotificationSettingFacilities(setting: INotificationSetting, apiContext: IApiContext) {
        val facilities = listOf(NotificationFacility.NORMAL, NotificationFacility.QUICK_MESSAGE, NotificationFacility.MAIL)
        editSystemNotificationFacilities(setting, NotificationFacilityState(facilities, emptyList()), apiContext)
    }

    fun disableAllSystemNotificationSettingFacilities(setting: INotificationSetting, apiContext: IApiContext) {
        val facilities = listOf(NotificationFacility.NORMAL, NotificationFacility.QUICK_MESSAGE, NotificationFacility.MAIL)
        editSystemNotificationFacilities(setting, NotificationFacilityState(emptyList(), facilities), apiContext)
    }

    fun resetDeleteResponse() {
        _systemNotificationDeleteResponse.value = null
    }

    fun resetBatchDeleteResponse() {
        _systemNotificationBatchDeleteResponse.value = null
    }

    fun resetSystemNotificationSettingsResponse() {
        _systemNotificationSettingsResponse.value = null
    }

}