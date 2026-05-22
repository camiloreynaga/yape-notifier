package com.yapenotifier.android.data.api

import android.content.Context
import com.yapenotifier.android.BuildConfig
import com.yapenotifier.android.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object RetrofitClient {
    private val BASE_URL = BuildConfig.API_BASE_URL
    
    // Professional approach: Cache token in memory to avoid runBlocking in interceptor
    // This prevents potential deadlocks and improves performance
    private val tokenCache = AtomicReference<String?>(null)
    private val tokenCacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var tokenObserverInitialized = false

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Solo mostrar logs detallados en modo debug
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    fun createApiService(context: Context): ApiService {
        // Validar que la URL esté configurada correctamente
        if (BASE_URL.isBlank()) {
            Timber.tag("RetrofitClient").e("API_BASE_URL no está configurada en BuildConfig!")
            throw IllegalStateException("API_BASE_URL no está configurada. Verifica build.gradle.kts")
        }

        // Validar que la URL de producción use HTTPS
        if (!BuildConfig.DEBUG && !BASE_URL.startsWith("https://")) {
            Timber.tag("RetrofitClient").w("⚠️ ADVERTENCIA: URL de producción no usa HTTPS: $BASE_URL")
        }

        Timber.tag("RetrofitClient").d("Creating API service with base URL: $BASE_URL")
        Timber.tag("RetrofitClient").d("Build type: ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")

        val preferencesManager = PreferencesManager(context)
        
        // Initialize token cache synchronously on first call
        initializeTokenCache(preferencesManager)
        
        // Initialize token observer only once (singleton pattern)
        if (!tokenObserverInitialized) {
            synchronized(this) {
                if (!tokenObserverInitialized) {
                    observeTokenChanges(preferencesManager)
                    tokenObserverInitialized = true
                }
            }
        }

        val authInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()

            // Professional approach: Use cached token instead of runBlocking
            // This prevents deadlocks and improves performance
            val token = tokenCache.get()

            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

            // Add authorization token if available
            token?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val response = chain.proceed(requestBuilder.build())

            // Detect 401 ONLY if we actually sent a token (avoid loops on anonymous requests).
            // AuthSessionManager handles the rest asynchronously — never blocks the interceptor.
            if (response.code == 401 && token != null) {
                Timber.tag("RetrofitClient").w("401 on ${originalRequest.url.encodedPath} — dispatching to AuthSessionManager")
                com.yapenotifier.android.util.FileLogger.log(
                    "AUTH",
                    "401 on ${originalRequest.url.encodedPath} (token present) — dispatched to AuthSessionManager",
                    "error",
                )
                com.yapenotifier.android.data.auth.AuthSessionManager.handleTokenExpiredAsync(
                    context = context,
                    endpoint = originalRequest.url.encodedPath,
                )
            }

            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(RetryInterceptor(maxRetries = 3, initialDelayMs = 1000L))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Asegurar que la URL base termine con /
        val normalizedBaseUrl = if (BASE_URL.endsWith("/")) BASE_URL else "$BASE_URL/"
        
        Timber.tag("RetrofitClient").d("Base URL normalizada: $normalizedBaseUrl")
        Timber.tag("RetrofitClient").d("URL completa para link-by-code: ${normalizedBaseUrl}api/devices/link-by-code")

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
    
    /**
     * Initialize token cache synchronously on first API service creation.
     * This is a one-time blocking operation that happens during app initialization.
     */
    private fun initializeTokenCache(preferencesManager: PreferencesManager) {
        if (tokenCache.get() == null) {
            try {
                val token = runBlocking {
                    preferencesManager.authToken.first()
                }
                tokenCache.set(token)
                Timber.tag("RetrofitClient").d("Token cache initialized")
            } catch (e: Exception) {
                Timber.tag("RetrofitClient").w(e, "Failed to initialize token cache")
            }
        }
    }
    
    /**
     * Observe token changes and update cache asynchronously.
     * This ensures the cache stays up-to-date without blocking the interceptor.
     */
    private fun observeTokenChanges(preferencesManager: PreferencesManager) {
        tokenCacheScope.launch {
            try {
                preferencesManager.authToken.collect { token ->
                    tokenCache.set(token)
                    Timber.tag("RetrofitClient").d("Token cache updated")
                }
            } catch (e: Exception) {
                Timber.tag("RetrofitClient").e(e, "Error observing token changes")
            }
        }
    }
    
    /**
     * Clear token cache (useful for logout).
     */
    fun clearTokenCache() {
        tokenCache.set(null)
        Timber.tag("RetrofitClient").d("Token cache cleared")
    }

    /**
     * Update the in-memory token cache explicitly. Used by AuthSessionManager to
     * eliminate the race between DataStore writes and observeTokenChanges flow.
     */
    fun updateTokenCache(token: String?) {
        tokenCache.set(token)
        Timber.tag("RetrofitClient").d("Token cache updated explicitly (token present: ${token != null})")
    }
}
