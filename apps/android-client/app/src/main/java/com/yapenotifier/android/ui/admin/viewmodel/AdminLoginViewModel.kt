package com.yapenotifier.android.ui.admin.viewmodel

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
import com.yapenotifier.android.data.repository.CommerceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AdminLoginResult(
    val success: Boolean,
    val message: String? = null,
    val needsCommerceCreation: Boolean = false
)

@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager,
    private val commerceRepository: CommerceRepository
) : AndroidViewModel(application) {

    private val _loginResult = MutableLiveData<AdminLoginResult?>()
    val loginResult: LiveData<AdminLoginResult?> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(emailOrPhone: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Professional approach: Use ApiCallHandler for type-safe error handling
            val loginResult = ApiCallHandler.safeApiCall(getApplication()) {
                val request = LoginRequest(emailOrPhone, password)
                apiService.login(request)
            }
            
            when (loginResult) {
                is ApiResult.Success -> {
                    val authResponse = loginResult.data
                    
                    // Verify user has admin role
                    val userResult = ApiCallHandler.safeApiCall(getApplication()) {
                        apiService.getCurrentUser()
                    }
                    
                    when (userResult) {
                        is ApiResult.Success -> {
                            val user = userResult.data
                            if (user.role != "admin") {
                                _loginResult.value = AdminLoginResult(
                                    false,
                                    "Solo los administradores pueden acceder a este portal"
                                )
                                _isLoading.value = false
                                return@launch
                            }
                        }
                        else -> {
                            Timber.w("Could not verify user role, proceeding with login")
                        }
                    }
                    
                    preferencesManager.saveAuthToken(authResponse.token)
                    preferencesManager.saveUserEmail(authResponse.user.email)
                    
                    // Check if commerce exists
                    val commerceResult = ApiCallHandler.safeApiCall(getApplication()) {
                        apiService.checkCommerce()
                    }
                    
                    when (commerceResult) {
                        is ApiResult.Success -> {
                            val body = commerceResult.data
                            val needsCreation = !body.hasCommerce
                            Timber.d("Commerce check: hasCommerce=${body.hasCommerce}, commerceId=${body.commerceId}, needsCreation=$needsCreation")
                            _loginResult.value = AdminLoginResult(
                                true,
                                "Login exitoso",
                                needsCreation
                            )
                        }
                        is ApiResult.HttpError -> {
                            // Si es 401/403, es error de autenticación
                            if (commerceResult.code == 401 || commerceResult.code == 403) {
                                _loginResult.value = AdminLoginResult(
                                    false,
                                    "Error de autenticación al verificar comercio"
                                )
                            } else {
                                // Otro error, asumimos que no tiene commerce
                                _loginResult.value = AdminLoginResult(
                                    true,
                                    "Login exitoso",
                                    needsCommerceCreation = true
                                )
                            }
                        }
                        else -> {
                            // Error de red u otro, ir al panel
                            Timber.e("Error al verificar commerce, navegando al panel")
                            _loginResult.value = AdminLoginResult(
                                true,
                                "Login exitoso",
                                needsCommerceCreation = false
                            )
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    _loginResult.value = AdminLoginResult(
                        false,
                        loginResult.getErrorMessage() ?: "Error de conexión"
                    )
                }
                is ApiResult.HttpError -> {
                    _loginResult.value = AdminLoginResult(
                        false,
                        loginResult.getErrorMessage() ?: "Error de autenticación"
                    )
                }
                is ApiResult.UnknownError -> {
                    _loginResult.value = AdminLoginResult(
                        false,
                        loginResult.getErrorMessage() ?: "Error desconocido"
                    )
                }
                is ApiResult.Loading -> {
                    // No debería llegar aquí
                }
            }
            
            _isLoading.value = false
        }
    }
}



