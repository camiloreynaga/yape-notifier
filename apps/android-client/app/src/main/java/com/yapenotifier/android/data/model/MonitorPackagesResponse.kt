package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response wrapper for monitor packages list.
 */
data class MonitorPackagesResponse(
    @SerializedName("packages")
    val packages: List<MonitorPackage>
)

