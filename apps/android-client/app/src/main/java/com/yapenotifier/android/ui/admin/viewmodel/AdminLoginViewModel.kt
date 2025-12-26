package com.yapenotifier.android.ui.admin.viewmodel

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.LoginRequest
import com.yapenotifier.android.data.repository.CommerceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AdminLoginResult(
    val success: Boolean,
    val message: String? = null,
    val needsCommerceCreation: Boolean = false
)

class AdminLoginViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val apiService = RetrofitClient.createApiService(application)
    private val preferencesManager = PreferencesManager(application)
    private val commerceRepository = CommerceRepository(apiService)

    private val _loginResult = MutableLiveData<AdminLoginResult?>()
    val loginResult: LiveData<AdminLoginResult?> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(emailOrPhone: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = LoginRequest(emailOrPhone, password)
                val response = apiService.login(request)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Verify user has admin role
                        val userResponse = apiService.getCurrentUser()
                        if (userResponse.isSuccessful) {
                            val user = userResponse.body()
                            if (user?.role != "admin") {
                                _loginResult.value = AdminLoginResult(
                                    false,
                                    "Solo los administradores pueden acceder a este portal"
                                )
                                return@launch
                            }
                        } else {
                            // If we can't verify role, check from auth response user object
                            // For now, we'll allow login and check role from backend later
                            Log.w("AdminLoginViewModel", "Could not verify user role, proceeding with login")
                        }

                        preferencesManager.saveAuthToken(authResponse.token)
                        preferencesManager.saveUserEmail(authResponse.user.email)

                        // Check if commerce exists
                        val commerceCheckResponse = commerceRepository.checkCommerce()
                        if (commerceCheckResponse.isSuccessful) {
                            val needsCreation = commerceCheckResponse.body()?.exists == false
                            _loginResult.value = AdminLoginResult(
                                true,
                                "Login exitoso",
                                needsCreation
                            )
                        } else {
                            _loginResult.value = AdminLoginResult(
                                false,
                                "Error al verificar el comercio"
                            )
                        }
                    } else {
                        _loginResult.value = AdminLoginResult(false, "Respuesta de login inválida del servidor")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _loginResult.value = AdminLoginResult(false, "Error de login: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _loginResult.value = AdminLoginResult(false, "Error de conexión: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}



