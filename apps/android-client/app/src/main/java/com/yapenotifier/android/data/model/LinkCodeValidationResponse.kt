package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class LinkCodeValidationResponse(
    val valid: Boolean,
    val message: Any,
    val commerce: CommerceInfo?
)

data class CommerceInfo(
    val id: Long,
    val name: String
)

data class LinkDeviceRequest(
    val code: String,
    @SerializedName("device_uuid")
    val deviceUuid: String,
    @SerializedName("device_name")
    val deviceName: String? = null
)

data class LinkDeviceResponse(
    val message: String,
    val device: Device?
)

