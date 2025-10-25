package com.example.andalib

data class Book(
    val id: Int = 0,
    val title: String,
    val author: String,
    val publisher: String,
    val year: String,
    val category: String,
    val coverPath: String
)