package com.yapenotifier.android.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.RegisterRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RegisterResult(
    val success: Boolean,
    val message: String? = null
)

class RegisterViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val apiService = RetrofitClient.createApiService(application)
    private val preferencesManager = PreferencesManager(application)

    private val _registerResult = MutableLiveData<RegisterResult?>()
    val registerResult: LiveData<RegisterResult?> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun register(name: String, email: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = RegisterRequest(name, email, password, passwordConfirmation)
                val response = apiService.register(request)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Save token and user info
                        preferencesManager.saveAuthToken(authResponse.token)
                        preferencesManager.saveUserEmail(authResponse.user.email)
                        
                        // Register device
                        registerDevice()

                        _registerResult.value = RegisterResult(true, "Registro exitoso")
                    } else {
                        _registerResult.value = RegisterResult(false, "Respuesta inválida del servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _registerResult.value = RegisterResult(false, "Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _registerResult.value = RegisterResult(false, "Error de conexión: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun registerDevice() {
        try {
            val deviceUuid = preferencesManager.deviceUuid.first()
                ?: kotlinx.coroutines.runBlocking {
                    val uuid = java.util.UUID.randomUUID().toString()
                    preferencesManager.saveDeviceUuid(uuid)
                    uuid
                }

            val deviceName = android.os.Build.MODEL ?: "Android Device"
            val createDeviceRequest = com.yapenotifier.android.data.model.CreateDeviceRequest(
                uuid = deviceUuid,
                name = deviceName,
                platform = "android"
            )

            val deviceResponse = apiService.createDevice(createDeviceRequest)
            if (deviceResponse.isSuccessful) {
                val device = deviceResponse.body()?.get("device") as? com.yapenotifier.android.data.model.Device
                device?.id?.let { deviceId ->
                    preferencesManager.saveDeviceId(deviceId.toString())
                }
            }
        } catch (e: Exception) {
            // Log error but don't block registration
            android.util.Log.e("RegisterViewModel", "Error registering device", e)
        }
    }
}
