package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Device log entry from the server
 */
data class DeviceLogEntry(
    val id: Long,
    @SerializedName("device_id")
    val deviceId: Long,
    @SerializedName("commerce_id")
    val commerceId: Long?,
    val category: String,
    val level: String,
    val message: String,
    val details: String?,
    @SerializedName("device_timestamp")
    val deviceTimestamp: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val device: DeviceBasic? = null
)

/**
 * Basic device info included in log entries
 */
data class DeviceBasic(
    val id: Long,
    val name: String,
    val alias: String?,
    val uuid: String
)

/**
 * Summary response for device logs
 */
data class DeviceLogsSummaryResponse(
    val hours: Int,
    val devices: List<DeviceSummary>
)

/**
 * Summary of logs per device
 */
data class DeviceSummary(
    val device: DeviceBasic,
    val counts: Map<String, Int>,
    val total: Int,
    @SerializedName("has_disconnects")
    val hasDisconnects: Boolean,
    @SerializedName("has_errors")
    val hasErrors: Boolean
)
