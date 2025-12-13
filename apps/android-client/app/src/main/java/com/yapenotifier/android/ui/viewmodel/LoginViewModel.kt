package com.yapenotifier.android.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.AuthResponse
import com.yapenotifier.android.data.model.LoginRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginResult(
    val success: Boolean,
    val message: String? = null
)

class LoginViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val apiService = RetrofitClient.createApiService(application)
    private val preferencesManager = PreferencesManager(application)

    private val _loginResult = MutableLiveData<LoginResult?>()
    val loginResult: LiveData<LoginResult?> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Save token and user info
                        preferencesManager.saveAuthToken(authResponse.token)
                        authResponse.user.email.let { // Safe call '?' removed
                            preferencesManager.saveUserEmail(it)
                        }
                        
                        // Register device
                        registerDevice()

                        _loginResult.value = LoginResult(true, "Login exitoso")
                    } else {
                        _loginResult.value = LoginResult(false, "Respuesta inválida del servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _loginResult.value = LoginResult(false, "Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult(false, "Error de conexión: ${e.message}")
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
            // Log error but don't block login
            android.util.Log.e("LoginViewModel", "Error registering device", e)
        }
    }
}
