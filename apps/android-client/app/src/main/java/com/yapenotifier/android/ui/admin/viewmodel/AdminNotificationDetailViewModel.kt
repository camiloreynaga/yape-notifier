package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiCallHandler
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.ApiResult
import com.yapenotifier.android.data.model.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AdminNotificationDetailUiState(
    val notification: Notification? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val statusUpdated: Boolean = false
)

@HiltViewModel
class AdminNotificationDetailViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<AdminNotificationDetailUiState>(AdminNotificationDetailUiState())
    val uiState: LiveData<AdminNotificationDetailUiState> = _uiState

    fun loadNotification(notificationId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null, statusUpdated = false)

            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.getNotification(notificationId)
            }

            when (result) {
                is ApiResult.Success -> {
                    val notification = result.data
                    Timber.d("AdminNotificationDetailViewModel: Notificación cargada - id=${notification.id}, status=${notification.status}")
                    _uiState.value = AdminNotificationDetailUiState(
                        notification = notification,
                        loading = false
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

    fun updateStatus(status: String) {
        val currentNotification = _uiState.value?.notification ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(loading = true, error = null, statusUpdated = false)

            val result = ApiCallHandler.safeApiCall(getApplication()) {
                apiService.updateNotificationStatus(
                    currentNotification.id,
                    mapOf("status" to status)
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    Timber.d("AdminNotificationDetailViewModel: Estado actualizado - id=${currentNotification.id}, nuevoStatus=$status")
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        statusUpdated = true
                    )
                    // Reload notification to get updated status
                    loadNotification(currentNotification.id)
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
}

