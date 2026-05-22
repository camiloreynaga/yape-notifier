package com.yapenotifier.android.data.model

data class LoginPinResponse(
    val message: String,
    val token: String,
    val user: UserData
)

data class UserData(
    val id: Long,
    val name: String,
    val email: String?,
    val role: String,
    val commerceId: Long
)

