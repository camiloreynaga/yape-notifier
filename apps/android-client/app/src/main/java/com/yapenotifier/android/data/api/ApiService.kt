package com.yapenotifier.android.data.api

import com.yapenotifier.android.data.model.AuthResponse
import com.yapenotifier.android.data.model.CreateDeviceRequest
import com.yapenotifier.android.data.model.DeviceResponse
import com.yapenotifier.android.data.model.LoginRequest
import com.yapenotifier.android.data.model.MonitoredPackagesResponse
import com.yapenotifier.android.data.model.NotificationData
import com.yapenotifier.android.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // --- Auth ---
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/logout")
    suspend fun logout(): Response<Unit>

    // --- Devices ---
    @POST("api/devices")
    suspend fun createDevice(@Body request: CreateDeviceRequest): Response<DeviceResponse>

    // --- Notifications ---
    @POST("api/notifications")
    suspend fun createNotification(@Body notificationData: NotificationData): Response<Unit>

    // --- Settings ---
    @GET("api/settings/monitored-packages")
    suspend fun getMonitoredPackages(): Response<MonitoredPackagesResponse>
}
