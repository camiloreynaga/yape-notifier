package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class Commerce(
    val id: String,
    val name: String,
    val country: String,
    val currency: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
