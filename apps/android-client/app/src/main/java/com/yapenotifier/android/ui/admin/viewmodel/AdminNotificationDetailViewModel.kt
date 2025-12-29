package com.yapenotifier.android.ui.admin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
            try {
                _uiState.value = _uiState.value?.copy(loading = true, error = null)

                val response = apiService.getNotification(notificationId)

                if (response.isSuccessful) {
                    val notification = response.body()
                    _uiState.value = AdminNotificationDetailUiState(
                        notification = notification,
                        loading = false
                    )
                } else {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = "Error al cargar la notificación: ${response.code()}"
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

    fun updateStatus(status: String) {
        val currentNotification = _uiState.value?.notification ?: return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value?.copy(loading = true, error = null)

                val response = apiService.updateNotificationStatus(
                    currentNotification.id,
                    mapOf("status" to status)
                )

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        statusUpdated = true
                    )
                    // Reload notification to get updated status
                    loadNotification(currentNotification.id)
                } else {
                    _uiState.value = _uiState.value?.copy(
                        loading = false,
                        error = "Error al actualizar el estado: ${response.code()}"
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
}

