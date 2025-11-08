package com.example.andalib.data.signup

data class SignUpResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: UserData? // Opsional: Untuk menerima data user yang baru dibuat
)

data class UserData(
    val id: Int,
    val name: String,
    val email: String
)