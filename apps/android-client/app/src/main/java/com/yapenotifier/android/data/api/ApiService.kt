package com.yapenotifier.android.data.api

import com.yapenotifier.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/logout")
    suspend fun logout(): Response<Map<String, String>>

    @GET("api/me")
    suspend fun getMe(): Response<Map<String, User>>

    // Devices
    @GET("api/devices")
    suspend fun getDevices(): Response<Map<String, List<Device>>>

    @POST("api/devices")
    suspend fun createDevice(@Body request: CreateDeviceRequest): Response<Map<String, Device>>

    // Notifications
    @POST("api/notifications")
    suspend fun createNotification(@Body notification: NotificationData): Response<Map<String, Any>>
}

