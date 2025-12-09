package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class NotificationData(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("source_app")
    val sourceApp: String,
    val title: String?,
    val body: String,
    val amount: Double?,
    val currency: String? = "PEN",
    @SerializedName("payer_name")
    val payerName: String?,
    @SerializedName("received_at")
    val receivedAt: String,
    @SerializedName("raw_json")
    val rawJson: Map<String, Any>? = null,
    val status: String? = "pending"
)

