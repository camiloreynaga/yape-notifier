package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class CommerceCheckResponse(
    @SerializedName("has_commerce")
    val hasCommerce: Boolean,
    @SerializedName("commerce_id")
    val commerceId: Long? = null
)
