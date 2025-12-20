package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class DeviceHealthData(
    @SerializedName("battery_level")
    val batteryLevel: Int?,
    
    @SerializedName("battery_optimization_disabled")
    val batteryOptimizationDisabled: Boolean?,
    
    @SerializedName("notification_permission_enabled")
    val notificationPermissionEnabled: Boolean?
)

