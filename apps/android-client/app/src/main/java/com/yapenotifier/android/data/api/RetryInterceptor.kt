package com.yapenotifier.android.data.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Interceptor profesional para retry automático de requests fallidos.
 * Implementa exponential backoff para evitar sobrecargar el servidor.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        
        Timber.tag("RetryInterceptor").d("Iniciando request a ${request.url}")
        
        // Intentar hasta maxRetries veces
        for (attempt in 0..maxRetries) {
            try {
                // Si no es el primer intento, esperar antes de reintentar
                if (attempt > 0) {
                    val delayMs = initialDelayMs * (1 shl (attempt - 1)) // Exponential backoff
                    Timber.tag("RetryInterceptor").d(
                        "Retry attempt $attempt for ${request.url} after ${delayMs}ms"
                    )
                    Thread.sleep(delayMs)
                }
                
                Timber.tag("RetryInterceptor").d("Intento ${attempt + 1}/${maxRetries + 1} para ${request.url}")
                response = chain.proceed(request)
                
                // Si la respuesta es exitosa o no es retryable, retornar
                if (response.isSuccessful) {
                    Timber.tag("RetryInterceptor").d("Request exitoso en intento ${attempt + 1}")
                    return response
                } else if (!isRetryable(response.code)) {
                    // Error no retryable (ej: 400, 401, 403, 404)
                    val errorBody = response.peekBody(2048).string() // Peek para no consumir el body
                    Timber.tag("RetryInterceptor").w(
                        "Error HTTP ${response.code} no retryable para ${request.url}. Body: $errorBody"
                    )
                    return response
                }
                
                // Si llegamos aquí, la respuesta no fue exitosa pero es retryable
                // Capturar el body del error antes de cerrar la respuesta
                val errorBody = try {
                    response.peekBody(2048).string() // Peek para no consumir el body
                } catch (e: Exception) {
                    "No se pudo leer el body del error: ${e.message}"
                }
                
                Timber.tag("RetryInterceptor").w(
                    "Error HTTP ${response.code} retryable en intento ${attempt + 1} para ${request.url}. Body: $errorBody"
                )
                response.close()
                response = null
                
            } catch (e: SocketTimeoutException) {
                lastException = IOException("Timeout en intento ${attempt + 1}: ${e.message}", e)
                Timber.tag("RetryInterceptor").w(
                    lastException,
                    "Timeout en ${request.url}, intento ${attempt + 1}/${maxRetries + 1}"
                )
                
            } catch (e: IOException) {
                // Preservar el mensaje original del error
                lastException = IOException(
                    "Error de red en intento ${attempt + 1}: ${e.message ?: e.javaClass.simpleName}",
                    e
                )
                Timber.tag("RetryInterceptor").w(
                    lastException,
                    "Error de red en ${request.url}, intento ${attempt + 1}/${maxRetries + 1}. Causa: ${e.message}"
                )
                
            } catch (e: Exception) {
                // Capturar cualquier otra excepción inesperada
                lastException = IOException(
                    "Error inesperado en intento ${attempt + 1}: ${e.message ?: e.javaClass.simpleName}",
                    e
                )
                Timber.tag("RetryInterceptor").e(
                    lastException,
                    "Excepción inesperada en ${request.url}, intento ${attempt + 1}/${maxRetries + 1}"
                )
            }
            
            // Si es el último intento, lanzar la excepción con el mensaje original preservado
            if (attempt == maxRetries) {
                response?.close()
                val finalException = lastException ?: IOException(
                    "Error desconocido después de ${maxRetries + 1} intentos para ${request.url}"
                )
                Timber.tag("RetryInterceptor").e(
                    finalException,
                    "Falló después de ${maxRetries + 1} intentos para ${request.url}"
                )
                throw finalException
            }
        }
        
        // Esto no debería ejecutarse, pero por seguridad
        response?.close()
        val finalException = lastException ?: IOException(
            "Error desconocido después de todos los intentos para ${request.url}"
        )
        Timber.tag("RetryInterceptor").e(finalException, "Falló después de todos los intentos")
        throw finalException
    }
    
    /**
     * Determina si un código HTTP es retryable.
     * Professional approach: Solo retry en errores del servidor o timeouts.
     */
    private fun isRetryable(httpCode: Int): Boolean {
        return when (httpCode) {
            // Timeout
            408 -> true
            // Rate limiting (con retry)
            429 -> true
            // Server errors (5xx)
            in 500..599 -> true
            // Gateway errors
            502, 503, 504 -> true
            // No retry en errores del cliente (4xx excepto los mencionados)
            else -> false
        }
    }
}

