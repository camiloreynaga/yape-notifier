package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single app instance with its details.
 */
data class AppInstance(
    @SerializedName("id")
    val id: Long,

    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("instance_label")
    var label: String?
)
