package com.example.andalib.screen.pengembalian

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun createCameraImageUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File.createTempFile("bukti_", ".jpg", cameraDir)

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider", // ✅ SAMA seperti modul anggota
        imageFile
    )
}