package com.example.andalib.data.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
fun Context.getFileName(uri: Uri): String {
    var name = "bukti_${System.currentTimeMillis()}.jpg"
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && idx >= 0) {
            name = it.getString(idx)
        }
    }
    return name
}

fun uriToMultipart(context: Context, uri: Uri, partName: String = "buktiKerusakan"): MultipartBody.Part {
    val contentResolver = context.contentResolver
    val mime = contentResolver.getType(uri) ?: "image/*"

    val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
    contentResolver.openInputStream(uri).use { input ->
        FileOutputStream(tempFile).use { output ->
            if (input != null) input.copyTo(output)
        }
    }

    val reqBody = tempFile.asRequestBody(mime.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, tempFile.name, reqBody)
}