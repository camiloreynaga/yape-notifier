package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.Device
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDevicesUiState(
    val devices: List<Device> = emptyList(),
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
            try {
                _uiState.value = _uiState.value?.copy(loading = true, error = null)

                val response = apiService.getDevices()

                if (response.isSuccessful) {
                    val devicesResponse = response.body()
                    val devices = devicesResponse?.devices ?: emptyList()
                    _uiState.value = AdminDevicesUiState(
                        devices = devices,
                        loading = false
                    )
                } else {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = "Error al cargar dispositivos: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    loading = false,
                    error = e.message ?: "Error de conexión"
                )
            }
        }
    }

    fun deleteDevice(deviceId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteDevice(deviceId)
                
                if (response.isSuccessful) {
                    // After successful deletion, reload devices
                    loadDevices()
                } else {
                    _uiState.value = _uiState.value?.copy(
                        error = "Error al eliminar dispositivo: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    error = e.message ?: "Error al eliminar dispositivo"
                )
            }
        }
    }

    fun updateDeviceAlias(deviceId: Long, alias: String) {
        viewModelScope.launch {
            try {
                val response = apiService.updateDevice(deviceId, mapOf("name" to alias))
                
                if (response.isSuccessful) {
                    // After successful update, reload devices
                    loadDevices()
                } else {
                    _uiState.value = _uiState.value?.copy(
                        error = "Error al actualizar dispositivo: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    error = e.message ?: "Error al actualizar dispositivo"
                )
            }
        }
    }
}

