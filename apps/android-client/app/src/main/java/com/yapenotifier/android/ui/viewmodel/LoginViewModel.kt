package com.yapenotifier.android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.LoginRequest
import com.yapenotifier.android.data.repository.CommerceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginResult(
    val success: Boolean,
    val message: String? = null,
    val needsCommerceCreation: Boolean = false
)

class LoginViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val apiService = RetrofitClient.createApiService(application)
    private val preferencesManager = PreferencesManager(application)
    private val commerceRepository = CommerceRepository(apiService)

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
                        preferencesManager.saveAuthToken(authResponse.token)
                        preferencesManager.saveUserEmail(authResponse.user.email)

                        val deviceRegistered = registerDevice()
                        if (deviceRegistered) {
                            val commerceCheckResponse = commerceRepository.checkCommerce()
                            if(commerceCheckResponse.isSuccessful) {
                                val needsCreation = commerceCheckResponse.body()?.exists == false
                                _loginResult.value = LoginResult(true, "Login exitoso y dispositivo registrado.", needsCreation)
                            } else {
                                _loginResult.value = LoginResult(false, "Error al verificar el comercio.")
                            }
                        } else {
                            _loginResult.value = LoginResult(false, "Error al registrar el dispositivo. Por favor, intente de nuevo.")
                        }
                    } else {
                        _loginResult.value = LoginResult(false, "Respuesta de login inválida del servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _loginResult.value = LoginResult(false, "Error de login: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _loginResult.value = LoginResult(false, "Error de conexión: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun registerDevice(): Boolean {
        try {
            val deviceUuid = preferencesManager.deviceUuid.first() ?: run {
                val uuid = java.util.UUID.randomUUID().toString()
                preferencesManager.saveDeviceUuid(uuid)
                Log.d("LoginViewModel", "Generated new device UUID: $uuid")
                uuid
            }

            Log.d("LoginViewModel", "Attempting to register device with UUID: $deviceUuid")

            val deviceName = android.os.Build.MODEL ?: "Android Device"
            val createDeviceRequest = com.yapenotifier.android.data.model.CreateDeviceRequest(
                uuid = deviceUuid,
                name = deviceName,
                platform = "android"
            )

            val deviceResponse = apiService.createDevice(createDeviceRequest)
            
            if (deviceResponse.isSuccessful) {
                val responseBody = deviceResponse.body()
                Log.d("LoginViewModel", "Device registration response body: $responseBody")

                responseBody?.device?.let {
                    preferencesManager.saveDeviceId(it.id.toString())
                    Log.i("LoginViewModel", "Device successfully registered with server. Saved remote ID: ${it.id}")
                    return true
                } ?: run {
                    Log.e("LoginViewModel", "Could not find 'device' object in the response body.")
                    return false
                }
            } else {
                val errorBody = deviceResponse.errorBody()?.string()
                Log.e("LoginViewModel", "Device registration API call failed. Code: ${deviceResponse.code()}, Body: $errorBody")
                return false
            }
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Exception during device registration", e)
            return false
        }
    }
}
