package com.yapenotifier.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a paginated response from the API.
 */
data class PaginatedResponse<T>(
    @SerializedName("data")
    val data: List<T>,

    @SerializedName("current_page")
    val currentPage: Int,

    @SerializedName("last_page")
    val lastPage: Int,

    @SerializedName("per_page")
    val perPage: Int,

    @SerializedName("total")
    val total: Int,

    @SerializedName("from")
    val from: Int?,

    @SerializedName("to")
    val to: Int?
)

