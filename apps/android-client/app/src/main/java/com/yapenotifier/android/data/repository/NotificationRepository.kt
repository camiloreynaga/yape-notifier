package com.yapenotifier.android.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.yapenotifier.android.BuildConfig
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.NotificationData
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.flow.first

/**
 * Resultado del envío de notificación con detalles del error
 */
sealed class SendResult {
    data class Success(val message: String = "Notificación enviada correctamente") : SendResult()
    data class Error(val httpCode: Int? = null, val message: String, val isAuthError: Boolean = false) : SendResult()
    data class NetworkError(val exception: Exception) : SendResult()
}

/**
 * Estado de autenticación del usuario
 */
enum class AuthStatus {
    AUTHENTICATED,      // Usuario autenticado con token válido
    NOT_AUTHENTICATED,  // No hay token de autenticación
    TOKEN_EXPIRED,      // Token expirado (401)
    NO_DEVICE           // No hay dispositivo registrado
}

class NotificationRepository(private val context: Context) {
    private val apiService = RetrofitClient.createApiService(context)
    private val preferencesManager = PreferencesManager(context)

    /**
     * Verifica si el usuario está autenticado (tiene token)
     */
    suspend fun isAuthenticated(): Boolean {
        val token = preferencesManager.authToken.first()
        return !token.isNullOrBlank()
    }

    /**
     * Verifica si el dispositivo está registrado
     */
    suspend fun hasDeviceRegistered(): Boolean {
        val deviceId = preferencesManager.deviceId.first()
        return !deviceId.isNullOrBlank()
    }

    /**
     * Obtiene el estado de autenticación completo
     */
    suspend fun getAuthStatus(): AuthStatus {
        val token = preferencesManager.authToken.first()
        val deviceId = preferencesManager.deviceId.first()
        
        return when {
            token.isNullOrBlank() -> AuthStatus.NOT_AUTHENTICATED
            deviceId.isNullOrBlank() -> AuthStatus.NO_DEVICE
            else -> AuthStatus.AUTHENTICATED
        }
    }

    /**
     * Envía una notificación a la API con manejo detallado de errores
     */
    suspend fun sendNotification(notificationData: NotificationData): SendResult {
        Log.d(TAG, "Preparing to send notification data: ${Gson().toJson(notificationData)}")
        ServiceStatusManager.updateStatus("📤 Enviando a la API: ${notificationData.sourceApp}")

        return try {
            val response = apiService.createNotification(notificationData)
            
            if (response.isSuccessful) {
                Log.i(TAG, "API call successful. Code: ${response.code()}")
                ServiceStatusManager.updateStatus("✅ API OK: ${response.code()}")
                SendResult.Success("Notificación enviada correctamente")
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                val httpCode = response.code()
                val isAuthError = httpCode == 401 || httpCode == 403
                
                val errorMessage = when (httpCode) {
                    401 -> "No autorizado. Token inválido o expirado. Por favor, inicia sesión nuevamente."
                    403 -> "Acceso denegado. No tienes permisos para realizar esta acción."
                    404 -> "Recurso no encontrado. Verifica que el dispositivo esté registrado."
                    422 -> "Datos inválidos: $errorBody"
                    500 -> "Error del servidor. Intenta más tarde."
                    else -> "Error HTTP ${httpCode}: ${response.message()}"
                }
                
                Log.e(TAG, "API call failed. Code: $httpCode, Message: ${response.message()}, Body: $errorBody")
                ServiceStatusManager.updateStatus("🔥 API FAIL: $httpCode - ${response.message()}")
                
                SendResult.Error(httpCode, errorMessage, isAuthError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending notification", e)
            
            // Mejorar mensaje de error con más detalles
            val errorMessage = when {
                e is javax.net.ssl.SSLHandshakeException || 
                e.message?.contains("SSL", ignoreCase = true) == true ||
                e.message?.contains("certificate", ignoreCase = true) == true -> {
                    "Error SSL: Verifica certificado del servidor"
                }
                e is java.net.UnknownHostException -> {
                    "No se puede conectar al servidor. Verifica tu conexión a internet."
                }
                e is java.net.ConnectException -> {
                    "No se puede conectar al servidor. Verifica la URL: ${BuildConfig.API_BASE_URL}"
                }
                e is java.net.SocketTimeoutException -> {
                    "Timeout: El servidor no responde. Intenta más tarde."
                }
                e is java.io.IOException -> {
                    "Error de red: ${e.message ?: "Verifica tu conexión a internet"}"
                }
                else -> {
                    "Error de red: ${e.javaClass.simpleName} - ${e.message ?: "Error desconocido"}"
                }
            }
            
            ServiceStatusManager.updateStatus("🔥 $errorMessage")
            SendResult.NetworkError(e)
        }
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}
