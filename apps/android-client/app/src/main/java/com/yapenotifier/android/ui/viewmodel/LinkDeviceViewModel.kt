package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.yapenotifier.android.data.api.ApiCallHandler
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.ApiResult
import com.yapenotifier.android.data.model.CommerceInfo
import com.yapenotifier.android.data.model.LinkDeviceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LinkDeviceViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

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
            // Professional approach: Use ApiCallHandler
            val result = ApiCallHandler.safeApiCall(getApplication()) {
                val normalizedCode = code.trim().uppercase()
                apiService.validateLinkCode(normalizedCode)
            }

            when (result) {
                is ApiResult.Success -> {
                    val body = result.data
                    if (body.valid && body.commerce != null) {
                        _validationState.value = ValidationState.Valid(body.commerce)
                    } else {
                        _validationState.value = ValidationState.Invalid(body.message.toMessageString() ?: "Código inválido")
                    }
                }
                is ApiResult.NetworkError -> {
                    Timber.e(result.throwable, "Error validating code")
                    _validationState.value = ValidationState.Invalid(
                        result.getErrorMessage(getApplication()) ?: "Error de conexión"
                    )
                }
                is ApiResult.HttpError -> {
                    _validationState.value = ValidationState.Invalid(
                        result.getErrorMessage(getApplication()) ?: "Error al validar código"
                    )
                }
                is ApiResult.UnknownError -> {
                    Timber.e(result.throwable, "Error validating code")
                    _validationState.value = ValidationState.Invalid(
                        result.getErrorMessage(getApplication()) ?: "Error desconocido"
                    )
                }
                is ApiResult.Loading -> {
                    // Ya está en Validating
                }
            }
        }
    }

    /**
     * Link device to commerce using a link code.
     *
     * Professional Architecture Approach:
     * - Authentication is optional (flexible UX for capturer mode)
     * - Backend automatically creates device if it doesn't exist
     * - Link code is the primary authorization mechanism
     * - If user is authenticated, device is associated with user (traceability)
     */
    fun linkDevice(code: String) {
        _linkState.value = LinkState.Linking
        viewModelScope.launch {
            try {
                // Ensure device UUID exists (generated in Application.onCreate)
                // Uses ANDROID_ID for persistence across reinstalls
                val deviceUuid = preferencesManager.deviceUuid.first() ?: run {
                    // Fallback: generate UUID from ANDROID_ID if not in preferences
                    val androidId = Settings.Secure.getString(
                        getApplication<Application>().contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                    val uuid = if (!androidId.isNullOrBlank()) {
                        UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
                    } else {
                        UUID.randomUUID().toString()
                    }
                    preferencesManager.saveDeviceUuid(uuid)
                    Timber.d("LinkDeviceViewModel: Generado device UUID desde ANDROID_ID: $uuid")
                    uuid
                }

                if (deviceUuid.isBlank()) {
                    throw IllegalStateException("No se pudo generar Device UUID")
                }

                // Check if user is authenticated (optional - for traceability)
                val authToken = preferencesManager.authToken.first()
                val isAuthenticated = !authToken.isNullOrBlank()

                Timber.d("LinkDeviceViewModel: Intentando vincular dispositivo. UUID: $deviceUuid, Autenticado: $isAuthenticated")

                val normalizedCode = code.trim().uppercase()
                val deviceName = android.os.Build.MODEL ?: "Android Device"

                // Professional approach: Use ApiCallHandler for type-safe error handling
                val result = ApiCallHandler.safeApiCall(getApplication()) {
                    // Note: Backend will automatically create device if it doesn't exist
                    val request = LinkDeviceRequest(
                        code = normalizedCode,
                        deviceUuid = deviceUuid,
                        deviceName = deviceName
                    )
                    apiService.linkDeviceByCode(request)
                }

                when (result) {
                    is ApiResult.Success -> {
                        val body = result.data
                        // Save device_id and commerce_id if available in device response
                        body.device?.let { device ->
                            // CRITICAL: Save device ID first
                            preferencesManager.saveDeviceId(device.id.toString())
                            Timber.d("LinkDeviceViewModel: Device ID saved: ${device.id}")

                            device.commerceId?.let { commerceId ->
                                preferencesManager.saveCommerceId(commerceId.toString())
                                Timber.d("LinkDeviceViewModel: Device linked successfully: ${device.id}, Commerce ID: $commerceId")
                            } ?: run {
                                Timber.w("LinkDeviceViewModel: Device linked but no commerce_id")
                            }
                        } ?: run {
                            Timber.w("LinkDeviceViewModel: Device linked but no device object in response")
                        }
                        _linkState.value = LinkState.Success(body.message)

                    }
                    is ApiResult.HttpError -> {
                        val errorMessage = result.getErrorMessage(getApplication()) ?: "Error al vincular dispositivo"
                        Timber.e("LinkDeviceViewModel: Error linking device: code=${result.code}, message=$errorMessage")
                        _linkState.value = LinkState.Error(errorMessage)
                    }
                    is ApiResult.NetworkError -> {
                        val errorMessage = result.getErrorMessage(getApplication()) ?: "Error de conexión"
                        Timber.e(result.throwable, "LinkDeviceViewModel: Network error linking device")
                        _linkState.value = LinkState.Error(errorMessage)
                    }
                    is ApiResult.UnknownError -> {
                        val errorMessage = result.getErrorMessage(getApplication()) ?: "Error desconocido"
                        Timber.e(result.throwable, "LinkDeviceViewModel: Unknown error linking device")
                        _linkState.value = LinkState.Error(errorMessage)
                    }
                    is ApiResult.Loading -> {
                        // Already in Linking state
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "LinkDeviceViewModel: Exception linking device")
                _linkState.value = LinkState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    fun resetValidationState() {
        _validationState.value = ValidationState.Idle
    }

    fun resetLinkState() {
        _linkState.value = LinkState.Idle
    }

    private fun Any.toMessageString(): String? {
        if (this is String) {
            return this
        }
        return try {
            Gson().toJson(this)
        } catch (e: Exception) {
            this.toString()
        }
    }
}
