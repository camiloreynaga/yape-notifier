package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.AppInstance
import com.yapenotifier.android.data.model.UpdateAppInstanceLabelRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AppInstancesUiState(
    val instances: List<AppInstance> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null
)

class AppInstancesViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = RetrofitClient.createApiService(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableLiveData<AppInstancesUiState>(AppInstancesUiState())
    val uiState: LiveData<AppInstancesUiState> = _uiState

    fun loadAppInstances(deviceId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null)
            
            try {
                val response = apiService.getDeviceAppInstances(deviceId)
                
                if (response.isSuccessful) {
                    val instances = response.body()?.instances ?: emptyList()
                    _uiState.value = _uiState.value?.copy(
                        instances = instances,
                        loading = false
                    )
                } else {
                    val errorMessage = "Error al cargar instancias: ${response.code()}"
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e("AppInstancesViewModel", "Error loading app instances", e)
                _uiState.value = _uiState.value?.copy(
                    loading = false,
                    error = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun updateInstanceLabel(instanceId: String, label: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(saving = true, saveError = null)
            
            try {
                val request = UpdateAppInstanceLabelRequest(instanceLabel = label.trim())
                val response = apiService.updateAppInstanceLabel(instanceId.toLongOrNull() ?: 0, request)
                
                if (response.isSuccessful) {
                    val updatedInstance = response.body()?.instance
                    if (updatedInstance != null) {
                        // Update the instance in the list
                        val currentInstances = _uiState.value?.instances ?: emptyList()
                        val updatedInstances = currentInstances.map { instance ->
                            if (instance.id.toString() == instanceId) {
                                updatedInstance
                            } else {
                                instance
                            }
                        }
                        _uiState.value = _uiState.value?.copy(
                            instances = updatedInstances,
                            saving = false
                        )
                    } else {
                        _uiState.value = _uiState.value?.copy(
                            saving = false,
                            saveError = "Respuesta inválida del servidor"
                        )
                    }
                } else {
                    val errorMessage = "Error al guardar: ${response.code()}"
                    _uiState.value = _uiState.value?.copy(
                        saving = false,
                        saveError = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e("AppInstancesViewModel", "Error updating instance label", e)
                _uiState.value = _uiState.value?.copy(
                    saving = false,
                    saveError = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun saveAllLabels(labels: Map<String, String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(saving = true, saveError = null)
            
            try {
                val updateJobs = labels.map { (instanceId, label) ->
                    launch {
                        val request = UpdateAppInstanceLabelRequest(instanceLabel = label.trim())
                        apiService.updateAppInstanceLabel(instanceId.toLongOrNull() ?: 0L, request)
                    }
                }
                
                updateJobs.forEach { it.join() }
                
                // Reload instances to get updated data
                val deviceIdStr = preferencesManager.deviceId.first()
                val deviceId = deviceIdStr?.toLongOrNull()
                if (deviceId != null) {
                    loadAppInstances(deviceId)
                } else {
                    _uiState.value = _uiState.value?.copy(
                        saving = false,
                        saveError = "No se pudo obtener el ID del dispositivo"
                    )
                }
            } catch (e: Exception) {
                Log.e("AppInstancesViewModel", "Error saving all labels", e)
                _uiState.value = _uiState.value?.copy(
                    saving = false,
                    saveError = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    fun hasUnnamedInstances(): Boolean {
        return _uiState.value?.instances?.any { 
            val label = it.label
            label.isNullOrBlank()
        } ?: false
    }
}
