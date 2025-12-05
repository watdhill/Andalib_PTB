// file SignUpResponse.kt

package com.example.andalib.data.signup

data class SignUpResponse(
    // Backend kita mengirimkan token langsung di root
    val message: String,
    val token: String? = null,
    val success: Boolean = true // Asumsi sukses jika tidak ada error HTTP
)
