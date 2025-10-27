// app/src/main/java/com/example/andalib/Utils.kt

package com.example.andalib // <-- Package utama, TIDAK ADA .util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Overload untuk BookScreen (hanya 2 argumen)
@Suppress("unused")
fun saveImageToInternalStorage(context: Context, uri: Uri): String {
    // Menggunakan prefix default untuk cover buku
    return saveImageToInternalStorage(context, uri, "book_cover")
}

/**
 * Menyimpan gambar dari Uri ke penyimpanan internal aplikasi.
 * (Digunakan oleh MembersScreen dan BorrowingScreen, yang memerlukan 'prefix')
 */
fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)

    inputStream?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }

    return file.absolutePath
}

/**
 * Membuat file gambar sementara untuk keperluan kamera (digunakan oleh MembersScreen dan BorrowingScreen).
 */
fun createImageFile(context: Context, prefix: String): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "${prefix}_${timeStamp}.jpg"
    return File(context.filesDir, fileName)
}

/**
 * Mendapatkan tanggal hari ini.
 */
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return sdf.format(Date())
}

/**
 * Mendapatkan tanggal di masa depan (misalnya, 7 hari ke depan).
 */
fun getFutureDate(days: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, days)
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return sdf.format(calendar.time)
}