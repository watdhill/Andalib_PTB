package com.example.andalib.screen.member

data class Member(
    val id: Int = 0,
    val name: String,
    val nim: String,
    val gender: String,
    val faculty: String,
    val major: String,
    val contact: String,
    val email: String,
    val photoPath: String = "",
    val registrationDate: String = ""
)