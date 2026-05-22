package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiCallHandler
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.ApiResult
import com.yapenotifier.android.data.model.LoginRequest
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.repository.CommerceRepository
import com.yapenotifier.android.worker.SendNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LoginResult(
    val success: Boolean,
    val message: String? = null,
    val needsCommerceCreation: Boolean = false,
    val needsDeviceLinking: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager,
    private val commerceRepository: CommerceRepository
) : AndroidViewModel(application) {

    private val _loginResult = MutableLiveData<LoginResult?>()
    val loginResult: LiveData<LoginResult?> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Professional approach: Use ApiCallHandler for type-safe error handling
            val loginResult = ApiCallHandler.safeApiCall(getApplication()) {
                val request = LoginRequest(email, password)
                apiService.login(request)
            }
            
            when (loginResult) {
                is ApiResult.Success -> {
                    val authResponse = loginResult.data
                    com.yapenotifier.android.data.auth.AuthSessionManager.handleLoginSucceeded(
                        context = getApplication(),
                        token = authResponse.token,
                    )
                    preferencesManager.saveUserEmail(authResponse.user.email)

                    val deviceRegistrationResult = registerDevice()
                    if (deviceRegistrationResult.success) {
                        val commerceResult = ApiCallHandler.safeApiCall(getApplication()) {
                            apiService.checkCommerce()
                        }
                        
                        when (commerceResult) {
                            is ApiResult.Success -> {
                                val needsCreation = !commerceResult.data.hasCommerce
                                val deviceHasCommerce = deviceRegistrationResult.device?.commerceId != null
                                
                                if (needsCreation) {
                                    _loginResult.value = LoginResult(
                                        success = true,
                                        message = "Login exitoso y dispositivo registrado.",
                                        needsCommerceCreation = true,
                                        needsDeviceLinking = false
                                    )
                                } else if (!deviceHasCommerce) {
                                    _loginResult.value = LoginResult(
                                        success = true,
                                        message = "Login exitoso. Necesitas vincular tu dispositivo.",
                                        needsCommerceCreation = false,
                                        needsDeviceLinking = true
                                    )
                                    // Disparar Worker para enviar notificaciones pendientes
                                    triggerPendingNotificationsWorker()
                                } else {
                                    deviceRegistrationResult.device?.commerceId?.let {
                                        preferencesManager.saveCommerceId(it.toString())
                                    }
                                    _loginResult.value = LoginResult(
                                        success = true,
                                        message = "Login exitoso y dispositivo registrado.",
                                        needsCommerceCreation = false,
                                        needsDeviceLinking = false
                                    )
                                    // Disparar Worker para enviar notificaciones pendientes
                                    triggerPendingNotificationsWorker()
                                }
                            }
                            else -> {
                                _loginResult.value = LoginResult(
                                    false,
                                    commerceResult.getErrorMessage(getApplication()) ?: "Error al verificar el comercio."
                                )
                            }
                        }
                    } else {
                        _loginResult.value = LoginResult(
                            false,
                            deviceRegistrationResult.message ?: "Error al registrar el dispositivo. Por favor, intente de nuevo."
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _loginResult.value = LoginResult(
                        false,
                        loginResult.getErrorMessage(getApplication()) ?: "Error de conexión"
                    )
                }
                is ApiResult.HttpError -> {
                    _loginResult.value = LoginResult(
                        false,
                        loginResult.getErrorMessage(getApplication()) ?: "Error de autenticación"
                    )
                }
                is ApiResult.UnknownError -> {
                    _loginResult.value = LoginResult(
                        false,
                        loginResult.getErrorMessage(getApplication()) ?: "Error desconocido"
                    )
                }
                is ApiResult.Loading -> {
                    // No debería llegar aquí
                }
            }
            
            _isLoading.value = false
        }
    }

    private data class DeviceRegistrationResult(
        val success: Boolean,
        val device: com.yapenotifier.android.data.model.Device? = null,
        val message: String? = null
    )

    private suspend fun registerDevice(): DeviceRegistrationResult {
        try {
            // El UUID debería existir (generado en Application.onCreate)
            // Si no existe, es un error crítico
            val deviceUuid = preferencesManager.deviceUuid.first()
                ?: throw IllegalStateException("Device UUID no encontrado. La app debe reiniciarse.")

            Timber.tag("LoginViewModel").d("Attempting to register device with UUID: $deviceUuid")

            val deviceName = android.os.Build.MODEL ?: "Android Device"
            val createDeviceRequest = com.yapenotifier.android.data.model.CreateDeviceRequest(
                uuid = deviceUuid,
                name = deviceName,
                platform = "android"
            )

            val deviceResponse = apiService.createDevice(createDeviceRequest)
            
            if (deviceResponse.isSuccessful) {
                val responseBody = deviceResponse.body()
                Timber.tag("LoginViewModel").d("Device registration response body: $responseBody")

                responseBody?.device?.let {
                    preferencesManager.saveDeviceId(it.id.toString())
                    Timber.tag("LoginViewModel").i("Device successfully registered with server. Saved remote ID: ${it.id}, Commerce ID: ${it.commerceId}")
                    return DeviceRegistrationResult(true, it)
                } ?: run {
                    Timber.tag("LoginViewModel").e("Could not find 'device' object in the response body.")
                    return DeviceRegistrationResult(false, null, "Respuesta inválida del servidor")
                }
            } else {
                val errorBody = deviceResponse.errorBody()?.string()
                Timber.tag("LoginViewModel").e("Device registration API call failed. Code: ${deviceResponse.code()}, Body: $errorBody")
                return DeviceRegistrationResult(false, null, "Error al registrar dispositivo: ${deviceResponse.code()}")
            }
        } catch (e: Exception) {
            Timber.tag("LoginViewModel").e(e, "Exception during device registration")
            return DeviceRegistrationResult(false, null, "Error de conexión: ${e.message}")
        }
    }

    /**
     * Dispara el Worker para enviar notificaciones pendientes después del login exitoso
     */
    private fun triggerPendingNotificationsWorker() {
        val workManager = WorkManager.getInstance(getApplication())
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sendWorkRequest = OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(sendWorkRequest)
        Timber.tag("LoginViewModel").d("Worker para enviar notificaciones pendientes disparado después del login")
    }
}
