package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single app instance with its details.
 * Supports dual/clone apps by tracking android_user_id.
 * 
 * - android_user_id = 0 → Original app
 * - android_user_id > 0 (e.g., 999) → Clone/Dual app
 */
data class AppInstance(
    @SerializedName("id")
    val id: Long,

    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("android_user_id")
    val androidUserId: Int? = null,

    @SerializedName("instance_label")
    var label: String?
) {
    /**
     * Returns true if this is a clone/dual app instance.
     * Clone apps typically have android_user_id > 0 (e.g., 999, 150, etc.)
     */
    val isClone: Boolean
        get() = (androidUserId ?: 0) > 0

    /**
     * Returns a human-readable type label.
     */
    val typeLabel: String
        get() = if (isClone) "Clon" else "Original"

    /**
     * Returns the app name extracted from package name.
     * e.g., "com.bcp.innovacxion.yape.movil" → "Yape"
     */
    val appName: String
        get() = when {
            packageName.contains("yape", ignoreCase = true) -> "Yape"
            packageName.contains("plin", ignoreCase = true) -> "Plin"
            packageName.contains("interbank", ignoreCase = true) -> "Interbank"
            packageName.contains("bcp", ignoreCase = true) -> "BCP"
            packageName.contains("bbva", ignoreCase = true) -> "BBVA"
            packageName.contains("scotiabank", ignoreCase = true) -> "Scotiabank"
            packageName.contains("banbif", ignoreCase = true) -> "BanBif"
            else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
}
