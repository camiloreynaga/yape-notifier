package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.model.AppStatus
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.ServiceStatusManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    private val _logoutComplete = MutableLiveData<Boolean>()
    val logoutComplete: LiveData<Boolean> = _logoutComplete

    // --- AppStatus streams ---

    /** Polls notification-access permission every second. */
    private val permissionFlow: StateFlow<Boolean> = flow {
        val ctx: Context = application.applicationContext
        while (true) {
            val has = withContext(Dispatchers.IO) {
                NotificationAccessChecker.isNotificationAccessEnabled(ctx)
            }
            emit(has)
            delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Polls ServiceStatusManager.isServiceConnected() every second. */
    private val serviceConnectedFlow: StateFlow<Boolean> = flow {
        while (true) {
            emit(ServiceStatusManager.isServiceConnected())
            delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val manualActionRequiredFlow: kotlinx.coroutines.flow.Flow<Boolean> =
        ServiceStatusManager.recoveryModeFlow.map {
            it == ServiceStatusManager.ListenerRecoveryMode.MANUAL_ACTION_REQUIRED
        }

    /**
     * Composed AppStatus with explicit priority ordering:
     * TokenExpired > PermissionRevoked > ListenerDeadManualNeeded > ListenerReconnecting > CapturingOK.
     *
     * 'CapturingOK' is NEVER emitted while awaitingLogin == true.
     */
    val appStatus: StateFlow<AppStatus> = combine(
        preferencesManager.awaitingLogin,
        permissionFlow,
        serviceConnectedFlow,
        manualActionRequiredFlow,
    ) { awaiting, hasPermission, connected, manualNeeded ->
        when {
            awaiting      -> AppStatus.TokenExpired
            !hasPermission -> AppStatus.PermissionRevoked
            manualNeeded  -> AppStatus.ListenerDeadManualNeeded
            !connected    -> AppStatus.ListenerReconnecting
            else          -> AppStatus.CapturingOK
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppStatus.CapturingOK)

    fun showMessage(message: String) {
        _statusMessage.value = message
        _statusMessage.value = null
    }

    /**
     * Unlink device - calls API to unlink and then clears local data.
     * This makes sense for capturer mode where login is optional but device linking is required.
     */
    fun unlinkDevice() {
        viewModelScope.launch {
            try {
                // Get device ID before clearing
                val deviceId = preferencesManager.deviceId.first()

                if (!deviceId.isNullOrBlank()) {
                    // Call API to unlink device
                    try {
                        val response = apiService.unlinkDevice(deviceId.toLong())
                        if (response.isSuccessful) {
                            Timber.tag("MainViewModel").d("Device unlinked successfully via API")
                            _statusMessage.value = "Dispositivo desvinculado exitosamente"
                        } else {
                            Timber.tag("MainViewModel").w("API unlink failed: ${response.code()}, proceeding with local clear")
                            _statusMessage.value = "Advertencia: Error al desvincular en servidor, datos locales borrados"
                        }
                    } catch (e: Exception) {
                        Timber.tag("MainViewModel").e(e, "Error calling unlink API, proceeding with local clear")
                        _statusMessage.value = "Advertencia: Sin conexión, datos locales borrados"
                    }
                } else {
                    _statusMessage.value = "No hay dispositivo vinculado"
                }

                // Clear all local data including auth token so re-linking forces a fresh PIN login
                preferencesManager.clearDeviceId()
                preferencesManager.clearCommerceId()
                preferencesManager.clearIsAdminMode()
                preferencesManager.clearAuthToken()
                com.yapenotifier.android.data.api.RetrofitClient.clearTokenCache()

                Timber.tag("MainViewModel").d("Local device data cleared successfully")
            } catch (e: Exception) {
                Timber.tag("MainViewModel").e(e, "Error unlinking device")
                _statusMessage.value = "Error al desvincular dispositivo"
            }

            // Notify the UI that unlink is complete
            _logoutComplete.value = true
        }
    }
    
    /**
     * Full logout - clears all data including auth token.
     * Use this only if user explicitly wants to logout (not common in capturer mode).
     */
    fun logout() {
        viewModelScope.launch {
            try {
                apiService.logout()
            } catch (e: Exception) {
                // Log error but don't block logout
                Timber.tag("MainViewModel").e(e, "Error calling logout API")
            }

            // Always clear local data regardless of API call success
            preferencesManager.clearAll()
            
            // Clear token cache in RetrofitClient
            com.yapenotifier.android.data.api.RetrofitClient.clearTokenCache()
            
            // Notify the UI that logout is complete
            _logoutComplete.value = true
        }
    }
}
