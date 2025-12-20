package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the response from the API when fetching app instances for a device.
 */
data class AppInstancesResponse(
    @SerializedName("instances")
    val instances: List<AppInstance>
)
