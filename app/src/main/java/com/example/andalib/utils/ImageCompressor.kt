package com.example.andalib.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Utility untuk compress image sebelum upload
 * Resize ke max 1024px dan compress ke JPEG 80%
 */
object ImageCompressor {
    
    private const val MAX_SIZE = 1024
    private const val JPEG_QUALITY = 80
    
    /**
     * Compress image dari URI
     * @param context Android context
     * @param uri URI dari image (dari camera atau gallery)
     * @return File hasil compress yang siap diupload
     */
    fun compressImage(context: Context, uri: Uri): File {
        // Read bitmap from URI
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        // Calculate resize ratio
        val ratio = min(
            MAX_SIZE.toFloat() / bitmap.width,
            MAX_SIZE.toFloat() / bitmap.height
        )
        
        // Resize if image is larger than MAX_SIZE
        val resizedBitmap = if (ratio < 1) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }
        
        // Create temp file in cache directory
        val compressedFile = File(
            context.cacheDir,
            "compressed_${System.currentTimeMillis()}.jpg"
        )
        
        // Compress to JPEG and save
        FileOutputStream(compressedFile).use { outputStream ->
            resizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                JPEG_QUALITY,
                outputStream
            )
        }
        
        // Clean up
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        bitmap.recycle()
        
        return compressedFile
    }
    
    /**
     * Get file size in MB
     */
    fun getFileSizeMB(file: File): Float {
        return file.length() / (1024f * 1024f)
    }
}
