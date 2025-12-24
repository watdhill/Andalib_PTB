package com.example.andalib.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.andalib.R

/**
 * Helper class untuk manage book notifications
 */
class BookNotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "book_notifications_channel"
        private const val CHANNEL_NAME = "Book Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for book management activities"
        private const val NOTIFICATION_ID_BASE = 2000
        
        // Notification types
        const val TYPE_BOOK_ADDED = "book_added"
        const val TYPE_BOOK_UPDATED = "book_updated"
        const val TYPE_BOOK_DELETED = "book_deleted"
        const val TYPE_STOCK_ADDED = "stock_added"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("BookNotification", "Notification channel created: $CHANNEL_ID")
        }
    }
    
    /**
     * Show notification for book activities
     */
    fun showBookNotification(
        type: String,
        bookTitle: String,
        additionalInfo: String = ""
    ) {
        android.util.Log.d("BookNotification", "showBookNotification called - type: $type, title: $bookTitle")
        
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.w("BookNotification", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }
        
        val (title, message) = when (type) {
            TYPE_BOOK_ADDED -> {
                "Buku Baru Ditambahkan" to "\"$bookTitle\" berhasil ditambahkan ke perpustakaan"
            }
            TYPE_BOOK_UPDATED -> {
                "Buku Diperbarui" to "\"$bookTitle\" berhasil diperbarui"
            }
            TYPE_BOOK_DELETED -> {
                "Buku Dihapus" to "\"$bookTitle\" telah dihapus dari perpustakaan"
            }
            TYPE_STOCK_ADDED -> {
                "Stok Ditambahkan" to "Stok \"$bookTitle\" berhasil ditambahkan$additionalInfo"
            }
            else -> {
                "Andalib Library" to "Aktivitas buku: $bookTitle"
            }
        }
        
        android.util.Log.d("BookNotification", "Notification - title: $title, message: $message")
        
        // Intent to open app when notification is tapped
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_books", true) // Extra to navigate to BookScreen
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
        
        // Generate unique notification ID based on current time
        val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 1000).toInt()
        
        android.util.Log.d("BookNotification", "Showing notification with ID: $notificationId")
        
        // Show notification
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        android.util.Log.d("BookNotification", "Notification shown successfully")
    }
    
    /**
     * Cancel all book notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * Cancel specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + notificationId)
    }
}
