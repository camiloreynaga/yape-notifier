package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a notification from the backend API.
 */
data class Notification(
    @SerializedName("id")
    val id: Long,

    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("commerce_id")
    val commerceId: Long?,

    @SerializedName("device_id")
    val deviceId: Long,

    @SerializedName("source_app")
    val sourceApp: String,

    @SerializedName("package_name")
    val packageName: String?,

    @SerializedName("android_user_id")
    val androidUserId: Int?,

    @SerializedName("android_uid")
    val androidUid: Int?,

    @SerializedName("app_instance_id")
    val appInstanceId: Long?,

    @SerializedName("title")
    val title: String,

    @SerializedName("body")
    val body: String,

    @SerializedName("amount")
    val amount: Double?,

    @SerializedName("currency")
    val currency: String?,

    @SerializedName("payer_name")
    val payerName: String?,

    @SerializedName("posted_at")
    val postedAt: String?,

    @SerializedName("received_at")
    val receivedAt: String,

    @SerializedName("raw_json")
    val rawJson: Map<String, Any>?,

    @SerializedName("status")
    val status: String, // "pending", "validated", "inconsistent"

    @SerializedName("is_duplicate")
    val isDuplicate: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("device")
    val device: Device?,

    @SerializedName("app_instance")
    val appInstance: AppInstance?
)

