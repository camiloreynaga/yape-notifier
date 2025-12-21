package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request to create a new device.
 * Matches the CreateDeviceRequest validation rules from Laravel backend.
 */
data class CreateDeviceRequest(
    @SerializedName("uuid")
    val uuid: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("platform")
    val platform: String = "android",

    @SerializedName("is_active")
    val isActive: Boolean = true
)

