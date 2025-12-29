package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiCallHandler
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.ApiResult
import com.yapenotifier.android.data.model.AppInstance
import com.yapenotifier.android.data.model.Device
import com.yapenotifier.android.data.model.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AdminDevicesUiState(
    val devices: List<Device> = emptyList(),
    val deviceInstances: Map<Long, List<AppInstance>> = emptyMap(),
    val lastNotifications: Map<Long, Notification?> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AdminDevicesViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<AdminDevicesUiState>(AdminDevicesUiState())
    val uiState: LiveData<AdminDevicesUiState> = _uiState

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null)

            val devicesResult = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.getDevices()
            }

            when (devicesResult) {
                is ApiResult.Success -> {
                    val devices = devicesResult.data.devices
                    Timber.d("AdminDevicesViewModel: Dispositivos cargados - count=${devices.size}")

                    // Cargar instancias y última notificación para cada dispositivo en paralelo
                    val instancesMap = mutableMapOf<Long, List<AppInstance>>()
                    val lastNotificationsMap = mutableMapOf<Long, Notification?>()

                    devices.forEach { device ->
                        // Cargar instancias del dispositivo
                        val instancesResult = ApiCallHandler.safeApiCall(getApplication()) {
                            apiService.getDeviceAppInstances(device.id)
                        }
                        if (instancesResult is ApiResult.Success) {
                            instancesMap[device.id] = instancesResult.data.instances
                        }

                        // Cargar última notificación del dispositivo
                        val notificationsResult = ApiCallHandler.safeApiCall(getApplication()) {
                            apiService.getNotifications(
                                deviceId = device.id,
                                perPage = 1,
                                page = 1
                            )
                        }
                        if (notificationsResult is ApiResult.Success) {
                            val notifications = notificationsResult.data.data
                            lastNotificationsMap[device.id] = notifications.firstOrNull()
                        }
                    }

                    _uiState.value = AdminDevicesUiState(
                        devices = devices,
                        deviceInstances = instancesMap,
                        lastNotifications = lastNotificationsMap,
                        loading = false
                    )
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = devicesResult.getErrorMessage()
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = devicesResult.getErrorMessage()
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = devicesResult.getErrorMessage()
                    )
                }
                else -> {}
            }
        }
    }

    fun deleteDevice(deviceId: Long) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.deleteDevice(deviceId)
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminDevicesViewModel: Dispositivo eliminado exitosamente")
                    loadDevices()
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value?.copy(error = result.getErrorMessage())
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value?.copy(error = result.getErrorMessage())
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value?.copy(error = result.getErrorMessage())
                }
                else -> {}
            }
        }
    }

    fun updateDeviceAlias(deviceId: Long, alias: String) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.updateDevice(deviceId, mapOf("name" to alias))
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminDevicesViewModel: Alias del dispositivo actualizado exitosamente")
                    loadDevices()
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value?.copy(error = result.getErrorMessage())
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value?.copy(error = result.getErrorMessage())
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value?.copy(error = result.getErrorMessage())
                }
                else -> {}
            }
        }
    }
}

