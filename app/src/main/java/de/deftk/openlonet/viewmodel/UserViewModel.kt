package de.deftk.openlonet.viewmodel

import android.accounts.Account
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.lonet.api.auth.Credentials
import de.deftk.lonet.api.implementation.ApiContext
import de.deftk.lonet.api.model.feature.systemnotification.ISystemNotification
import de.deftk.lonet.api.request.handler.AutoLoginRequestHandler
import de.deftk.openlonet.api.Response
import de.deftk.openlonet.feature.overview.AbstractOverviewElement
import de.deftk.openlonet.repository.UserRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val userRepository: UserRepository) : ViewModel() {

    private val _loginResource = MutableLiveData<Response<ApiContext>>()
    val loginResource: LiveData<Response<ApiContext>> = _loginResource

    val apiContext: LiveData<ApiContext?> = loginResource.map { if (it is Response.Success) it.value else null }

    private val _overviewResource = MutableLiveData<Response<List<AbstractOverviewElement>>>()
    val overviewResponse: LiveData<Response<List<AbstractOverviewElement>>> = _overviewResource

    private val _systemNotificationsResource = MutableLiveData<Response<List<ISystemNotification>>>()
    val systemNotifications: LiveData<Response<List<ISystemNotification>>> = _systemNotificationsResource

    fun loginPassword(username: String, password: String) {
        viewModelScope.launch {
            val resource = userRepository.loginPassword(username, password)
            if (resource is Response.Success) {
                setupApiContext(resource.value, Credentials.fromPassword(username, password))
            }
            _loginResource.value = resource
        }
    }

    fun loginPasswordCreateToken(username: String, password: String) {
        viewModelScope.launch {
            val resource = userRepository.loginPasswordCreateToken(username, password)
            if (resource is Response.Success) {
                setupApiContext(resource.value.first, Credentials.fromToken(username, resource.value.second))
                _loginResource.value = resource.map { it.first }
            } else if (resource is Response.Failure) {
                _loginResource.value = Response.Failure(resource.exception)
            }
        }
    }

    fun loginToken(username: String, token: String) {
        viewModelScope.launch {
            val resource = userRepository.loginToken(username, token)
            if (resource is Response.Success) {
                setupApiContext(resource.value, Credentials.fromToken(username, token))
            }
            _loginResource.value = resource
        }
    }

    fun loginAccount(account: Account, token: String) {
        return loginToken(account.name, token)
    }

    private fun setupApiContext(apiContext: ApiContext, credentials: Credentials) {
        apiContext.setRequestHandler(AutoLoginRequestHandler(object : AutoLoginRequestHandler.LoginHandler<ApiContext> {
            override fun getCredentials(): Credentials = credentials

            override fun onLogin(context: ApiContext) {
                _loginResource.value = Response.Success(context)
            }
        }, ApiContext::class.java))
    }

    fun loadOverview(apiContext: ApiContext) {
        viewModelScope.launch {
            val resource = userRepository.getOverviewElements(apiContext)
            _overviewResource.value = resource
        }
    }

    fun loadSystemNotifications(apiContext: ApiContext) {
        viewModelScope.launch {
            val resource = userRepository.getSystemNotifications(apiContext)
            _systemNotificationsResource.value = resource
        }
    }

    fun deleteSystemNotification(systemNotification: ISystemNotification, apiContext: ApiContext): LiveData<Response<Unit>> {
        return liveData {
            val response = userRepository.deleteSystemNotification(systemNotification, apiContext)
            if (_systemNotificationsResource.value is Response.Success && response is Response.Success) {
                val systemNotifications = (_systemNotificationsResource.value as Response.Success<List<ISystemNotification>>).value.toMutableList()
                systemNotifications.remove(systemNotification)
                _systemNotificationsResource.value = Response.Success(systemNotifications)
            }
            //TODO add _postResponse livedata
            emit(response)
        }
    }

}