package com.yapenotifier.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.LoginPinRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PinLoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val userName: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }
    
    fun loginWithPin(pin: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                val request = LoginPinRequest(pin = pin)
                val response = apiService.loginWithPin(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    // Guardar token y datos del usuario
                    preferencesManager.saveAuthToken(body.token)
                    preferencesManager.saveUserId(body.user.id.toString())
                    preferencesManager.saveUserName(body.user.name)
                    preferencesManager.saveCommerceId(body.user.commerceId.toString())
                    preferencesManager.saveIsAdminMode(false) // Mark as Capturer mode for session persistence

                    Timber.d("PIN login successful: ${body.user.name}")
                    _loginState.value = LoginState.Success(body.user.name)
                    
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "PIN inválido. Verifica con tu administrador."
                        403 -> "Usuario no autorizado."
                        else -> "Error al iniciar sesión. Código: ${response.code()}"
                    }
                    
                    Timber.e("PIN login failed: ${response.code()}")
                    _loginState.value = LoginState.Error(errorMessage)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "PIN login exception")
                _loginState.value = LoginState.Error(
                    "Error de conexión. Verifica tu internet."
                )
            }
        }
    }
    
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}

