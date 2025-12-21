package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response when generating a link code.
 */
data class LinkCodeGenerateResponse(
    @SerializedName("link_code")
    val linkCode: String,

    @SerializedName("qr_code_data")
    val qrCodeData: String,

    @SerializedName("expires_at")
    val expiresAt: String,

    @SerializedName("device_alias")
    val deviceAlias: String
)

