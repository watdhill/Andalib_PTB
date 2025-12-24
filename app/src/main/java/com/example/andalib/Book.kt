package com.example.andalib

data class Book(
    val id: Int = 0,
    val isbn: String = "", // ISBN yang harus unik
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val year: String = "",
    val category: String = "",
    val coverPath: String = "",
    val stok: Int = 0, // Jumlah stok buku
    val serverId: Int? = null // ID dari server (MySQL) jika sudah disinkronkan
)