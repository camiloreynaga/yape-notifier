package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

data class UpdateAppInstanceLabelRequest(
    @SerializedName("instance_label")
    val instanceLabel: String
)
