package com.example.andalib.data.login

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String?
)