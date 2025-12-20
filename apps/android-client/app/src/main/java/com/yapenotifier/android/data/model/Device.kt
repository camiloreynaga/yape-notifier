package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class Device(
    val id: Long,
    val uuid: String,
    val name: String,
    val platform: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("last_seen_at")
    val lastSeenAt: String?,
    @SerializedName("commerce_id")
    val commerceId: Long? = null
)

data class CreateDeviceRequest(
    val uuid: String?,
    val name: String,
    val platform: String = "android",
    @SerializedName("is_active")
    val isActive: Boolean = true
)

data class DeviceResponse(
    val device: Device
)
