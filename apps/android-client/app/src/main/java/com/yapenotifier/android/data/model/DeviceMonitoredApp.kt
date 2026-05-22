package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class DeviceMonitoredApp(
    val id: String,
    @SerializedName("package_name") val packageName: String
)
