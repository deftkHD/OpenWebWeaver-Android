package de.deftk.openww.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.deftk.openww.android.api.Response
import de.deftk.openww.android.feature.AppFeature
import de.deftk.openww.android.feature.overview.AbstractOverviewElement
import de.deftk.openww.android.filter.SystemNotificationFilter
import de.deftk.openww.android.repository.UserRepository
import de.deftk.openww.api.model.IApiContext
import de.deftk.openww.api.model.feature.systemnotification.INotificationSetting
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
import de.deftk.openww.api.model.feature.systemnotification.NotificationFacility
import de.deftk.openww.api.model.feature.systemnotification.NotificationFacilityState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val userRepository: UserRepository) : ScopedViewModel(savedStateHandle) {

    private val _overviewResponse = registerProperty<Response<List<AbstractOverviewElement>>?>("overviewResponse", true)
    val overviewResponse: LiveData<Response<List<AbstractOverviewElement>>?> = _overviewResponse

    private val _systemNotificationsResponse = registerProperty<Response<List<ISystemNotification>>?>("systemNotificationsResponse", true)
    val allSystemNotificationsResponse: LiveData<Response<List<ISystemNotification>>?> = _systemNotificationsResponse

    val systemNotificationFilter = registerProperty<SystemNotificationFilter>("systemNotificationFilter", true)

    private val _systemNotificationSettingsResponse = registerProperty<Response<List<INotificationSetting>>?>("systemNotificationSettingsResponse", true)
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