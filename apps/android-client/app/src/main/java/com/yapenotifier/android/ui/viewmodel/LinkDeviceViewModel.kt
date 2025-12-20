package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.CommerceInfo
import com.yapenotifier.android.data.model.LinkDeviceRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LinkDeviceViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = RetrofitClient.createApiService(application)
    private val preferencesManager = PreferencesManager(application)

    sealed class ValidationState {
        object Idle : ValidationState()
        object Validating : ValidationState()
        data class Valid(val commerce: CommerceInfo) : ValidationState()
        data class Invalid(val message: String) : ValidationState()
    }

    sealed class LinkState {
        object Idle : LinkState()
        object Linking : LinkState()
        data class Success(val message: String) : LinkState()
        data class Error(val message: String) : LinkState()
    }

    private val _validationState = MutableLiveData<ValidationState>(ValidationState.Idle)
    val validationState: LiveData<ValidationState> = _validationState

    private val _linkState = MutableLiveData<LinkState>(LinkState.Idle)
    val linkState: LiveData<LinkState> = _linkState

    fun validateCode(code: String) {
        if (code.isBlank()) {
            _validationState.value = ValidationState.Invalid("Por favor ingresa un código")
            return
        }

        _validationState.value = ValidationState.Validating
        viewModelScope.launch {
            try {
                val normalizedCode = code.trim().uppercase()
                val response = apiService.validateLinkCode(normalizedCode)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.valid && body.commerce != null) {
                        _validationState.postValue(ValidationState.Valid(body.commerce))
                    } else {
                        _validationState.postValue(
                            ValidationState.Invalid(body?.message ?: "Código inválido")
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        // Try to parse error response
                        val errorResponse = com.google.gson.Gson().fromJson(
                            errorBody,
                            com.yapenotifier.android.data.model.LinkCodeValidationResponse::class.java
                        )
                        errorResponse.message
                    } catch (e: Exception) {
                        "Error al validar código: ${response.code()}"
                    }
                    _validationState.postValue(ValidationState.Invalid(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("LinkDeviceViewModel", "Error validating code", e)
                _validationState.postValue(ValidationState.Invalid("Error de conexión: ${e.message}"))
            }
        }
    }

    fun linkDevice(code: String) {
        _linkState.value = LinkState.Linking
        viewModelScope.launch {
            try {
                val deviceUuid = preferencesManager.deviceUuid.first()
                    ?: throw IllegalStateException("Device UUID no encontrado")

                val normalizedCode = code.trim().uppercase()
                val request = LinkDeviceRequest(
                    code = normalizedCode,
                    deviceUuid = deviceUuid
                )

                val response = apiService.linkDeviceByCode(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Save commerce_id if available in device response
                        body.device?.let { device ->
                            device.commerceId?.let { commerceId ->
                                preferencesManager.saveCommerceId(commerceId.toString())
                                Log.d("LinkDeviceViewModel", "Device linked successfully: ${device.id}, Commerce ID: $commerceId")
                            } ?: run {
                                Log.d("LinkDeviceViewModel", "Device linked successfully: ${device.id}, but no commerce_id")
                            }
                        }
                        _linkState.postValue(LinkState.Success(body.message))
                    } else {
                        _linkState.postValue(LinkState.Error("Respuesta vacía del servidor"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorResponse = com.google.gson.Gson().fromJson(
                            errorBody,
                            com.yapenotifier.android.data.model.LinkDeviceResponse::class.java
                        )
                        errorResponse.message
                    } catch (e: Exception) {
                        "Error al vincular dispositivo: ${response.code()}"
                    }
                    _linkState.postValue(LinkState.Error(errorMessage))
                }
            } catch (e: Exception) {
                Log.e("LinkDeviceViewModel", "Error linking device", e)
                _linkState.postValue(LinkState.Error("Error de conexión: ${e.message}"))
            }
        }
    }

    fun resetValidationState() {
        _validationState.value = ValidationState.Idle
    }

    fun resetLinkState() {
        _linkState.value = LinkState.Idle
    }
}

