package com.example.andalib.data.login

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: UserInfo? = null
)

data class UserInfo(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
)