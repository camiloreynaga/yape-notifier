package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class DeviceHealthData(
    @SerializedName("battery_level")
    val batteryLevel: Int?,

    @SerializedName("battery_optimization_disabled")
    val batteryOptimizationDisabled: Boolean?,

    @SerializedName("notification_permission_enabled")
    val notificationPermissionEnabled: Boolean?,

    // NEW: Real service connection state
    @SerializedName("notification_service_connected")
    val notificationServiceConnected: Boolean?,

    // NEW: Count of pending notifications (not yet sent to API)
    @SerializedName("pending_notifications_count")
    val pendingNotificationsCount: Int?,

    // NEW: Timestamp of last captured notification
    @SerializedName("last_notification_captured_at")
    val lastNotificationCapturedAt: Long?
)

