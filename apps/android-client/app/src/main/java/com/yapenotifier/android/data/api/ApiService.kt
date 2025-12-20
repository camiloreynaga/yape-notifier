package com.yapenotifier.android.data.api

import com.yapenotifier.android.data.model.AuthResponse
import com.yapenotifier.android.data.model.CommerceCheckResponse
import com.yapenotifier.android.data.model.CommerceResponse
import com.yapenotifier.android.data.model.CreateCommerceRequest
import com.yapenotifier.android.data.model.CreateDeviceRequest
import com.yapenotifier.android.data.model.DeviceMonitoredAppsResponse
import com.yapenotifier.android.data.model.DeviceResponse
import com.yapenotifier.android.data.model.AppInstancesResponse
import com.yapenotifier.android.data.model.LinkCodeValidationResponse
import com.yapenotifier.android.data.model.LinkDeviceRequest
import com.yapenotifier.android.data.model.LinkDeviceResponse
import com.yapenotifier.android.data.model.LoginRequest
import com.yapenotifier.android.data.model.MonitoredPackagesResponse
import com.yapenotifier.android.data.model.NotificationData
import com.yapenotifier.android.data.model.RegisterRequest
import com.yapenotifier.android.data.model.UpdateAppInstanceLabelRequest
import com.yapenotifier.android.data.model.UpdateAppInstanceLabelResponse
import com.yapenotifier.android.data.model.UpdateDeviceMonitoredAppsRequest
import com.yapenotifier.android.data.model.DeviceHealthData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

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

    @GET("api/devices/{deviceId}/monitored-apps")
    suspend fun getDeviceMonitoredApps(@Path("deviceId") deviceId: String): Response<DeviceMonitoredAppsResponse>

    @POST("api/devices/{deviceId}/monitored-apps")
    suspend fun updateDeviceMonitoredApps(
        @Path("deviceId") deviceId: String,
        @Body request: UpdateDeviceMonitoredAppsRequest
    ): Response<Unit>

    // --- Commerce ---
    @POST("api/commerces")
    suspend fun createCommerce(@Body request: CreateCommerceRequest): Response<CommerceResponse>

    @GET("api/commerces/me")
    suspend fun getCommerce(): Response<CommerceResponse>

    @GET("api/commerces/check")
    suspend fun checkCommerce(): Response<CommerceCheckResponse>

    // --- Notifications ---
    @POST("api/notifications")
    suspend fun createNotification(@Body notificationData: NotificationData): Response<Unit>

    // --- Settings ---
    @GET("api/settings/monitored-packages")
    suspend fun getMonitoredPackages(): Response<MonitoredPackagesResponse>

    // --- Device Linking ---
    @GET("api/devices/link-code/{code}")
    suspend fun validateLinkCode(@Path("code") code: String): Response<LinkCodeValidationResponse>

    @POST("api/devices/link-by-code")
    suspend fun linkDeviceByCode(@Body request: LinkDeviceRequest): Response<LinkDeviceResponse>

    // --- App Instances ---
    @GET("api/devices/{deviceId}/app-instances")
    suspend fun getDeviceAppInstances(@Path("deviceId") deviceId: Long): Response<AppInstancesResponse>

    @PATCH("api/app-instances/{instanceId}/label")
    suspend fun updateAppInstanceLabel(
        @Path("instanceId") instanceId: Long,
        @Body request: UpdateAppInstanceLabelRequest
    ): Response<UpdateAppInstanceLabelResponse>

    // --- Device Health ---
    @POST("api/devices/{deviceId}/health")
    suspend fun updateDeviceHealth(
        @Path("deviceId") deviceId: String,
        @Body data: DeviceHealthData
    ): Response<Unit>
}
