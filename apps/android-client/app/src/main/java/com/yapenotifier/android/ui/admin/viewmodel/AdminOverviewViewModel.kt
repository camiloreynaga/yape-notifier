package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiCallHandler
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.ApiResult
import com.yapenotifier.android.data.model.Device
import com.yapenotifier.android.data.model.NotificationStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AdminOverviewUiState(
    val statistics: NotificationStatistics? = null,
    val devices: List<Device> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val activeDevices: Int = 0,
    val totalDevices: Int = 0
)

@HiltViewModel
class AdminOverviewViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<AdminOverviewUiState>(AdminOverviewUiState())
    val uiState: LiveData<AdminOverviewUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null)
            
            // Cargar estadísticas y dispositivos en paralelo
            val statsResult = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.getNotificationStatistics()
            }
            
            val devicesResult = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.getDevices()
            }

            when (statsResult) {
                is ApiResult.Success -> {
                    val stats = statsResult.data
                    Timber.d("AdminOverviewViewModel: Estadísticas cargadas - total=${stats.total}, amount=${stats.totalAmount}")
                    
                    when (devicesResult) {
                        is ApiResult.Success -> {
                            val devicesResponse = devicesResult.data
                            val devices = devicesResponse.devices
                            val activeDevices = devices.count { it.isActive }
                            
                            _uiState.value = AdminOverviewUiState(
                                statistics = stats,
                                devices = devices,
                                loading = false,
                                activeDevices = activeDevices,
                                totalDevices = devices.size
                            )
                        }
                        is ApiResult.HttpError -> {
                            _uiState.value = _uiState.value?.copy(
                                statistics = stats,
                                loading = false,
                                error = devicesResult.getErrorMessage()
                            )
                        }
                        is ApiResult.NetworkError -> {
                            _uiState.value = _uiState.value?.copy(
                                statistics = stats,
                                loading = false,
                                error = devicesResult.getErrorMessage()
                            )
                        }
                        is ApiResult.UnknownError -> {
                            _uiState.value = _uiState.value?.copy(
                                statistics = stats,
                                loading = false,
                                error = devicesResult.getErrorMessage()
                            )
                        }
                        else -> {}
                    }
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = statsResult.getErrorMessage()
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = statsResult.getErrorMessage()
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = statsResult.getErrorMessage()
                    )
                }
                else -> {}
            }
        }
    }

    fun refresh() {
        loadData()
    }
}

