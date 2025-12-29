package com.yapenotifier.android.data.api

import com.yapenotifier.android.data.model.ApiResult
import com.yapenotifier.android.util.NetworkUtils
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

/**
 * Handler profesional para llamadas a API.
 * Centraliza el manejo de errores y conversión a ApiResult.
 */
object ApiCallHandler {
    
    /**
     * Ejecuta una llamada a API y retorna un ApiResult.
     * Professional approach: Manejo centralizado de errores con tipos específicos.
     * 
     * @param context Context para verificar conectividad
     * @param apiCall Función suspend que ejecuta la llamada a API
     * @return ApiResult con el resultado o error
     */
    suspend fun <T> safeApiCall(
        context: android.content.Context,
        apiCall: suspend () -> Response<T>
    ): ApiResult<T> {
        // Verificar conectividad antes de hacer el request
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Timber.tag("ApiCallHandler").w("No hay conectividad de red disponible")
            return ApiResult.NetworkError(
                message = "No hay conexión a internet. Por favor, verifica tu conexión.",
                throwable = null
            )
        }
        
        return try {
            val response = apiCall()
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body)
                } else {
                    Timber.tag("ApiCallHandler").w("Response exitoso pero body es null")
                    ApiResult.UnknownError(
                        IllegalStateException("Response body is null")
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.tag("ApiCallHandler").w(
                    "HTTP error ${response.code()}: $errorBody"
                )
                ApiResult.HttpError(
                    code = response.code(),
                    message = errorBody ?: "Error HTTP ${response.code()}",
                    errorBody = errorBody
                )
            }
        } catch (e: IOException) {
            Timber.tag("ApiCallHandler").e(e, "Network error during API call")
            ApiResult.NetworkError(
                message = "Error de conexión: ${e.message ?: "Error desconocido"}",
                throwable = e
            )
        } catch (e: Exception) {
            Timber.tag("ApiCallHandler").e(e, "Unknown error during API call")
            ApiResult.UnknownError(e)
        }
    }
    
    /**
     * Ejecuta una llamada a API que retorna Unit (sin body).
     */
    suspend fun safeApiCallUnit(
        context: android.content.Context,
        apiCall: suspend () -> Response<Unit>
    ): ApiResult<Unit> {
        return safeApiCall(context) {
            apiCall()
        }
    }
}

