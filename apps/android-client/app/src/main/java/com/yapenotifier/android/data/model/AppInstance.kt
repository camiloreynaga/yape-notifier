package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class AppInstance(
    val id: String,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("android_user_id") val androidUserId: String,
    var label: String?
)
