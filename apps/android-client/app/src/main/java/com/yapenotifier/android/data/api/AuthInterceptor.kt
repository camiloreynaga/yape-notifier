package com.yapenotifier.android.data.api

import com.yapenotifier.android.data.local.PreferencesManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class AuthInterceptor(private val preferencesManager: PreferencesManager) : Interceptor {

    // Cache the token to avoid blocking on every request
    @Volatile
    private var cachedToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Use cached token if available, otherwise fetch synchronously (only on first request)
        val token = cachedToken ?: runBlocking {
            preferencesManager.authToken.firstOrNull().also {
                cachedToken = it
                Timber.d("AuthInterceptor: Token loaded from preferences")
            }
        }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
        }

        return chain.proceed(newRequest)
    }

    /**
     * Update the cached token. Call this when token changes (login/logout).
     */
    fun updateToken(token: String?) {
        cachedToken = token
        Timber.d("AuthInterceptor: Token cache updated")
    }

    /**
     * Clear the cached token. Call this on logout.
     */
    fun clearToken() {
        cachedToken = null
        Timber.d("AuthInterceptor: Token cache cleared")
    }
}
