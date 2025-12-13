package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class MonitoredPackagesResponse(
    @SerializedName("packages")
    val packages: List<String>
)
