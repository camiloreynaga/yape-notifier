package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response wrapper for device creation/retrieval endpoints.
 */
data class DeviceResponse(
    @SerializedName("device")
    val device: Device
)

