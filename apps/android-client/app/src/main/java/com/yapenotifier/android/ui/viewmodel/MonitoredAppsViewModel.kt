package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.repository.MonitoredAppsRepository
import com.yapenotifier.android.ui.MonitoredAppCheckableItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MonitoredAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.createApiService(application)
    private val repository = MonitoredAppsRepository(apiService)
    private val preferencesManager = PreferencesManager(application)

    sealed class MonitoredAppsState {
        object Loading : MonitoredAppsState()
        data class Success(val items: List<MonitoredAppCheckableItem>) : MonitoredAppsState()
        data class Error(val message: String) : MonitoredAppsState()
    }

    sealed class SaveState {
        object Idle : SaveState()
        object Loading : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val _appsState = MutableLiveData<MonitoredAppsState>()
    val appsState: LiveData<MonitoredAppsState> = _appsState

    private val _saveState = MutableLiveData<SaveState>()
    val saveState: LiveData<SaveState> = _saveState

    fun loadMonitoredApps() {
        _appsState.value = MonitoredAppsState.Loading
        viewModelScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()
                if (deviceId == null) {
                    _appsState.postValue(MonitoredAppsState.Error("Device ID not found"))
                    return@launch
                }

                val availablePackagesResponse = repository.getAvailablePackages()
                val deviceAppsResponse = repository.getDeviceMonitoredApps(deviceId)

                if (availablePackagesResponse.isSuccessful && deviceAppsResponse.isSuccessful) {
                    val availablePackages = availablePackagesResponse.body()?.packages ?: emptyList()
                    val devicePackages = deviceAppsResponse.body()?.monitoredApps?.map { it.packageName } ?: emptyList()

                    val checkableItems = availablePackages.map {
                        MonitoredAppCheckableItem(packageName = it, isChecked = devicePackages.contains(it))
                    }
                    _appsState.postValue(MonitoredAppsState.Success(checkableItems))
                } else {
                    _appsState.postValue(MonitoredAppsState.Error("Failed to load app lists"))
                }

            } catch (e: Exception) {
                _appsState.postValue(MonitoredAppsState.Error(e.message ?: "An unknown error occurred"))
            }
        }
    }

    fun saveMonitoredApps(selectedPackages: List<String>) {
        _saveState.value = SaveState.Loading
        viewModelScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()
                if (deviceId == null) {
                    _saveState.postValue(SaveState.Error("Device ID not found"))
                    return@launch
                }
                val response = repository.updateDeviceMonitoredApps(deviceId, selectedPackages)
                if (response.isSuccessful) {
                    _saveState.postValue(SaveState.Success)
                } else {
                    _saveState.postValue(SaveState.Error(response.errorBody()?.string() ?: "Failed to save selection"))
                }
            } catch (e: Exception) {
                _saveState.postValue(SaveState.Error(e.message ?: "An unknown error occurred"))
            }
        }
    }
}
