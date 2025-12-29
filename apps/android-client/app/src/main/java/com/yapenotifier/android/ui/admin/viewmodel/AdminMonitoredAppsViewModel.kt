package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiCallHandler
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.ApiResult
import com.yapenotifier.android.data.model.MonitorPackage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AdminMonitoredAppsUiState(
    val packages: List<MonitorPackage> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val showActiveOnly: Boolean = false
)

@HiltViewModel
class AdminMonitoredAppsViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<AdminMonitoredAppsUiState>(AdminMonitoredAppsUiState())
    val uiState: LiveData<AdminMonitoredAppsUiState> = _uiState

    init {
        loadPackages()
    }

    fun loadPackages(activeOnly: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null, showActiveOnly = activeOnly)
            
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.getMonitorPackages(if (activeOnly) true else null)
            }

            when (result) {
                is ApiResult.Success -> {
                    val packages = result.data.packages
                    Timber.d("AdminMonitoredAppsViewModel: Paquetes cargados - count=${packages.size}")
                    _uiState.value = AdminMonitoredAppsUiState(
                        packages = packages,
                        loading = false,
                        showActiveOnly = activeOnly
                    )
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = result.getErrorMessage()
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = result.getErrorMessage()
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = result.getErrorMessage()
                    )
                }
                else -> {}
            }
        }
    }

    fun createPackage(packageName: String, appName: String, description: String?, priority: Int) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.createMonitorPackage(
                    mapOf(
                        "package_name" to packageName,
                        "app_name" to appName,
                        "description" to (description ?: ""),
                        "priority" to priority
                    )
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminMonitoredAppsViewModel: Paquete creado exitosamente")
                    loadPackages(_uiState.value?.showActiveOnly ?: false)
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

    fun updatePackage(id: Long, packageName: String, appName: String, description: String?, priority: Int) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.updateMonitorPackage(
                    id,
                    mapOf(
                        "package_name" to packageName,
                        "app_name" to appName,
                        "description" to (description ?: ""),
                        "priority" to priority
                    )
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminMonitoredAppsViewModel: Paquete actualizado exitosamente")
                    loadPackages(_uiState.value?.showActiveOnly ?: false)
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

    fun deletePackage(id: Long) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.deleteMonitorPackage(id)
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminMonitoredAppsViewModel: Paquete eliminado exitosamente")
                    loadPackages(_uiState.value?.showActiveOnly ?: false)
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

    fun togglePackageStatus(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.toggleMonitorPackageStatus(id, mapOf("is_active" to isActive))
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminMonitoredAppsViewModel: Estado del paquete actualizado")
                    loadPackages(_uiState.value?.showActiveOnly ?: false)
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

    fun bulkCreatePackages(packages: List<Map<String, Any>>) {
        viewModelScope.launch {
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.bulkCreateMonitorPackages(mapOf("packages" to packages))
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminMonitoredAppsViewModel: Paquetes creados en bulk exitosamente")
                    loadPackages(_uiState.value?.showActiveOnly ?: false)
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

    fun refresh() {
        loadPackages(_uiState.value?.showActiveOnly ?: false)
    }
}

