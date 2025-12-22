package com.example.andalib.data.login

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?, // Token ada langsung di root
    val user: UserInfo? = null // User info dari backend
)

data class UserInfo(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
)