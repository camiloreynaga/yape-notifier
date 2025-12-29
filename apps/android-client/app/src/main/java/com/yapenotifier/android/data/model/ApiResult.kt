package com.yapenotifier.android.data.model

/**
 * Sealed class para manejo profesional de resultados de API.
 * Permite type-safe error handling y mejor UX.
 */
sealed class ApiResult<out T> {
    /**
     * Request exitoso con datos
     */
    data class Success<T>(val data: T) : ApiResult<T>()
    
    /**
     * Error de red (sin conexión, timeout, etc.)
     */
    data class NetworkError(val message: String, val throwable: Throwable? = null) : ApiResult<Nothing>()
    
    /**
     * Error HTTP (4xx, 5xx)
     */
    data class HttpError(
        val code: Int,
        val message: String,
        val errorBody: String? = null
    ) : ApiResult<Nothing>()
    
    /**
     * Error desconocido o inesperado
     */
    data class UnknownError(val throwable: Throwable) : ApiResult<Nothing>()
    
    /**
     * Request en progreso
     */
    object Loading : ApiResult<Nothing>()
    
    /**
     * Helper para obtener el mensaje de error apropiado para mostrar al usuario.
     * Professional approach: Usa string resources cuando sea posible.
     */
    fun getErrorMessage(context: android.content.Context? = null): String? {
        return when (this) {
            is NetworkError -> message
            is HttpError -> {
                if (context != null) {
                    when (code) {
                        401 -> context.getString(com.yapenotifier.android.R.string.error_session_expired)
                        403 -> context.getString(com.yapenotifier.android.R.string.error_no_permissions)
                        404 -> context.getString(com.yapenotifier.android.R.string.error_resource_not_found)
                        500, 502, 503 -> context.getString(com.yapenotifier.android.R.string.error_server_error)
                        else -> message
                    }
                } else {
                    when (code) {
                        401 -> "Sesión expirada. Por favor, inicia sesión nuevamente."
                        403 -> "No tienes permisos para realizar esta acción."
                        404 -> "Recurso no encontrado."
                        500, 502, 503 -> "Error del servidor. Por favor, intenta más tarde."
                        else -> message
                    }
                }
            }
            is UnknownError -> {
                if (context != null) {
                    context.getString(com.yapenotifier.android.R.string.error_unexpected, throwable.message ?: "Desconocido")
                } else {
                    "Error inesperado: ${throwable.message}"
                }
            }
            else -> null
        }
    }
    
    /**
     * Helper para verificar si es un error recuperable
     */
    fun isRetryable(): Boolean {
        return when (this) {
            is NetworkError -> true
            is HttpError -> code in 500..599 || code == 408 || code == 429
            else -> false
        }
    }
}

