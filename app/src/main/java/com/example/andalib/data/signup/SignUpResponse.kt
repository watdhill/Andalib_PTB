// file SignUpResponse.kt

package com.example.andalib.data.signup

data class SignUpResponse(

    val message: String,
    val token: String? = null,
    val success: Boolean = true
)
