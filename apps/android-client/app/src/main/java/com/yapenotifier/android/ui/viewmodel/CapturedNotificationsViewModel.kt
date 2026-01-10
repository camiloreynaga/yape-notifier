package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.repository.AuthStatus
import com.yapenotifier.android.data.repository.NotificationRepository
import com.yapenotifier.android.worker.SendNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resultado del intento de reintentar notificaciones
 */
sealed class RetryResult {
    object Started : RetryResult()
    object NoAuth : RetryResult()
    object NoDevice : RetryResult()
}

@HiltViewModel
class CapturedNotificationsViewModel @Inject constructor(
    application: Application,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val capturedNotificationDao = AppDatabase.getDatabase(application).capturedNotificationDao()
    private val workManager = WorkManager.getInstance(application)
    private val repository = NotificationRepository(application)

    val allNotifications = capturedNotificationDao.getAllNotificationsFlow().asLiveData()

    private val _authStatus = MutableLiveData<AuthStatus>()
    val authStatus: LiveData<AuthStatus> = _authStatus

    private val _retryResult = MutableLiveData<RetryResult?>()
    val retryResult: LiveData<RetryResult?> = _retryResult

    init {
        // Cargar estado de autenticación inicial
        viewModelScope.launch {
            _authStatus.value = repository.getAuthStatus()
        }
    }

    fun retryFailedNotifications() {
        viewModelScope.launch {
            // Verificar autenticación antes de reintentar
            val currentAuthStatus = repository.getAuthStatus()
            _authStatus.value = currentAuthStatus

            when (currentAuthStatus) {
                AuthStatus.NOT_AUTHENTICATED, AuthStatus.TOKEN_EXPIRED -> {
                    _retryResult.value = RetryResult.NoAuth
                    return@launch
                }
                AuthStatus.NO_DEVICE -> {
                    _retryResult.value = RetryResult.NoDevice
                    return@launch
                }
                AuthStatus.AUTHENTICATED -> {
                    capturedNotificationDao.resetFailedNotifications()
                    scheduleSendNotificationWorker()
                    _retryResult.value = RetryResult.Started
                }
            }
        }
    }

    private fun scheduleSendNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sendWorkRequest = OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(sendWorkRequest)
    }

    /**
     * Refresca el estado de autenticación
     */
    fun refreshAuthStatus() {
        viewModelScope.launch {
            _authStatus.value = repository.getAuthStatus()
        }
    }
}
