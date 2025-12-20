package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the response from the API after updating an app instance's label.
 * It likely returns the full updated object.
 */
data class UpdateAppInstanceLabelResponse(
    val message: String,
    @SerializedName("instance")
    val instance: AppInstance
)
