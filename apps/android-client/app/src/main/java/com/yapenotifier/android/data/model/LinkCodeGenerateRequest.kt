package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request to generate a link code for device pairing.
 */
data class LinkCodeGenerateRequest(
    @SerializedName("device_alias")
    val deviceAlias: String
)

