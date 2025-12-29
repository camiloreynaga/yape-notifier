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
import com.yapenotifier.android.data.model.UpdateAppInstanceLabelRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AdminAppInstancesUiState(
    val instances: List<AppInstance> = emptyList(),
    val devices: List<Device> = emptyList(),
    val selectedDeviceId: Long? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val assignedInstances: List<AppInstance> = emptyList(),
    val unassignedInstances: List<AppInstance> = emptyList()
)

@HiltViewModel
class AdminAppInstancesViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<AdminAppInstancesUiState>(AdminAppInstancesUiState())
    val uiState: LiveData<AdminAppInstancesUiState> = _uiState

    init {
        loadData()
    }

    fun loadData(deviceId: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null, selectedDeviceId = deviceId)
            
            // Cargar dispositivos e instancias en paralelo
            val devicesResult = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.getDevices()
            }
            
            val instancesResult = ApiCallHandler.safeApiCall(getApplication()) {
                if (deviceId != null) {
                    apiService.getDeviceAppInstances(deviceId)
                } else {
                    apiService.getAppInstances(deviceId)
                }
            }

            when (devicesResult) {
                is ApiResult.Success -> {
                    val devices = devicesResult.data.devices
                    
                    when (instancesResult) {
                        is ApiResult.Success -> {
                            val instances = instancesResult.data.instances
                            val assigned = instances.filter { it.label != null && it.label!!.isNotBlank() }
                            val unassigned = instances.filter { it.label == null || it.label!!.isBlank() }
                            
                            Timber.d("AdminAppInstancesViewModel: Instancias cargadas - total=${instances.size}, assigned=${assigned.size}, unassigned=${unassigned.size}")
                            
                            _uiState.value = AdminAppInstancesUiState(
                                instances = instances,
                                devices = devices,
                                selectedDeviceId = deviceId,
                                loading = false,
                                assignedInstances = assigned,
                                unassignedInstances = unassigned
                            )
                        }
                        is ApiResult.HttpError -> {
                            _uiState.value = _uiState.value?.copy(
                                devices = devices,
                                loading = false,
                                error = instancesResult.getErrorMessage()
                            )
                        }
                        is ApiResult.NetworkError -> {
                            _uiState.value = _uiState.value?.copy(
                                devices = devices,
                                loading = false,
                                error = instancesResult.getErrorMessage()
                            )
                        }
                        is ApiResult.UnknownError -> {
                            _uiState.value = _uiState.value?.copy(
                                devices = devices,
                                loading = false,
                                error = instancesResult.getErrorMessage()
                            )
                        }
                        else -> {}
                    }
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

    fun updateInstanceLabel(instanceId: Long, label: String) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.updateAppInstanceLabel(instanceId, UpdateAppInstanceLabelRequest(label))
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminAppInstancesViewModel: Label de instancia actualizado")
                    loadData(_uiState.value?.selectedDeviceId)
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

    fun filterByDevice(deviceId: Long?) {
        loadData(deviceId)
    }

    fun refresh() {
        loadData(_uiState.value?.selectedDeviceId)
    }
}

