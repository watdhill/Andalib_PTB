package com.example.andalib

data class Borrowing(
    val id: Int = 0,
    val borrowerName: String,
    val nim: String,
    val major: String,
    val contact: String,
    val bookTitle: String,
    val author: String,
    val isbn: String,
    val identityPath: String,
    val borrowDate: String,
    val returnDate: String
)