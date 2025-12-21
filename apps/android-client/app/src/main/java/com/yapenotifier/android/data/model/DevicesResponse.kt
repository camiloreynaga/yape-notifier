package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response wrapper for GET /api/devices endpoint.
 */
data class DevicesResponse(
    @SerializedName("devices")
    val devices: List<Device>
)

