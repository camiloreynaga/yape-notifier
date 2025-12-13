package com.yapenotifier.android.data.api

import android.content.Context
import com.yapenotifier.android.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Usando un endpoint Mock para pruebas. Visita https://yape-test.free.beeceptor.com para ver los datos recibidos.
    private const val BASE_URL = "https://yape-test.free.beeceptor.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun createApiService(context: Context): ApiService {
        val preferencesManager = PreferencesManager(context)
        
        val authInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()
            
            // Get auth token synchronously
            val token = runBlocking {
                preferencesManager.authToken.first()
            }
            
            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")

            // Add authorization token if available
            token?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
