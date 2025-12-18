package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class NotificationData(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("source_app")
    val sourceApp: String,
    @SerializedName("package_name")
    val packageName: String? = null,
    @SerializedName("android_user_id")
    val androidUserId: Int? = null,
    @SerializedName("android_uid")
    val androidUid: Int? = null,
    val title: String?,
    val body: String,
    val amount: Double?,
    val currency: String? = "PEN",
    @SerializedName("payer_name")
    val payerName: String?,
    @SerializedName("posted_at")
    val postedAt: String? = null,
    @SerializedName("received_at")
    val receivedAt: String,
    @SerializedName("raw_json")
    val rawJson: Map<String, Any>? = null,
    val status: String? = "pending"
)

