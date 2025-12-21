package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a device from the backend API.
 * Matches the Device model structure from Laravel backend.
 */
data class Device(
    @SerializedName("id")
    val id: Long,

    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("uuid")
    val uuid: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("platform")
    val platform: String,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("last_seen_at")
    val lastSeenAt: String?,

    @SerializedName("battery_level")
    val batteryLevel: Int?,

    @SerializedName("battery_optimization_disabled")
    val batteryOptimizationDisabled: Boolean?,

    @SerializedName("notification_permission_enabled")
    val notificationPermissionEnabled: Boolean?,

    @SerializedName("last_heartbeat")
    val lastHeartbeat: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?,

    @SerializedName("commerce_id")
    val commerceId: Long? = null
)
