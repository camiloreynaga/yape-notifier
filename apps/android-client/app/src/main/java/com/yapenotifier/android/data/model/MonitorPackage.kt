package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a monitor package (app to monitor for payment notifications).
 */
data class MonitorPackage(
    @SerializedName("id")
    val id: Long,

    @SerializedName("commerce_id")
    val commerceId: Long?,

    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("app_name")
    val appName: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("enabled_default")
    val enabledDefault: Boolean?,

    @SerializedName("priority")
    val priority: Int?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
)

