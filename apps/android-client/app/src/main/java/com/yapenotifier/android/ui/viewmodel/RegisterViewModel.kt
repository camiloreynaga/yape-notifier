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
import com.yapenotifier.android.data.model.RegisterRequest
import com.yapenotifier.android.data.repository.CommerceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RegisterResult(
    val success: Boolean,
    val message: String? = null,
    val needsCommerceCreation: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager,
    private val commerceRepository: CommerceRepository
) : AndroidViewModel(application) {

    private val _registerResult = MutableLiveData<RegisterResult?>()
    val registerResult: LiveData<RegisterResult?> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun register(name: String, email: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Professional approach: Use ApiCallHandler for type-safe error handling
            val registerResult = ApiCallHandler.safeApiCall(getApplication()) {
                val request = RegisterRequest(name, email, password, passwordConfirmation)
                apiService.register(request)
            }
            
            when (registerResult) {
                is ApiResult.Success -> {
                    val authResponse = registerResult.data
                    preferencesManager.saveAuthToken(authResponse.token)
                    preferencesManager.saveUserEmail(authResponse.user.email)

                    val deviceRegistered = registerDevice()
                    if (deviceRegistered) {
                        val commerceResult = ApiCallHandler.safeApiCall(getApplication()) {
                            apiService.checkCommerce()
                        }
                        
                        when (commerceResult) {
                            is ApiResult.Success -> {
                                val needsCreation = !commerceResult.data.hasCommerce
                                _registerResult.value = RegisterResult(
                                    true,
                                    "Registro exitoso y dispositivo registrado.",
                                    needsCreation
                                )
                            }
                            else -> {
                                _registerResult.value = RegisterResult(
                                    false,
                                    commerceResult.getErrorMessage(getApplication()) ?: "Error al verificar el comercio."
                                )
                            }
                        }
                    } else {
                        _registerResult.value = RegisterResult(
                            false,
                            "Error al registrar el dispositivo. Por favor, intente de nuevo."
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _registerResult.value = RegisterResult(
                        false,
                        registerResult.getErrorMessage(getApplication()) ?: "Error de conexión"
                    )
                }
                is ApiResult.HttpError -> {
                    _registerResult.value = RegisterResult(
                        false,
                        registerResult.getErrorMessage(getApplication()) ?: "Error de registro"
                    )
                }
                is ApiResult.UnknownError -> {
                    _registerResult.value = RegisterResult(
                        false,
                        registerResult.getErrorMessage(getApplication()) ?: "Error desconocido"
                    )
                }
                is ApiResult.Loading -> {
                    // No debería llegar aquí
                }
            }
            
            _isLoading.value = false
        }
    }

    private suspend fun registerDevice(): Boolean {
        try {
            // El UUID debería existir (generado en Application.onCreate)
            // Si no existe, es un error crítico
            val deviceUuid = preferencesManager.deviceUuid.first()
                ?: throw IllegalStateException("Device UUID no encontrado. La app debe reiniciarse.")

            Timber.tag("RegisterViewModel").d("Attempting to register device with UUID: $deviceUuid")

            val deviceName = android.os.Build.MODEL ?: "Android Device"
            val createDeviceRequest = com.yapenotifier.android.data.model.CreateDeviceRequest(
                uuid = deviceUuid,
                name = deviceName,
                platform = "android"
            )

            val deviceResponse = apiService.createDevice(createDeviceRequest)
            
            if (deviceResponse.isSuccessful) {
                val responseBody = deviceResponse.body()
                Timber.tag("RegisterViewModel").d("Device registration response body: $responseBody")

                responseBody?.device?.let {
                    preferencesManager.saveDeviceId(it.id.toString())
                    Timber.tag("RegisterViewModel").i("Device successfully registered with server. Saved remote ID: ${it.id}")
                    return true
                } ?: run {
                    Timber.tag("RegisterViewModel").e("Could not find 'device' object in the response body.")
                    return false
                }
            } else {
                val errorBody = deviceResponse.errorBody()?.string()
                Timber.tag("RegisterViewModel").e("Device registration API call failed. Code: ${deviceResponse.code()}, Body: $errorBody")
                return false
            }
        } catch (e: Exception) {
            Timber.tag("RegisterViewModel").e(e, "Exception during device registration")
            return false
        }
    }
}
