package com.example.andalib.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.andalib.R
import com.example.andalib.screen.member.MemberNotification

/**
 * Helper class untuk manage member notifications
 */
class MemberNotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "member_notifications_channel"
        private const val CHANNEL_NAME = "Member Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for member management activities"
        private const val NOTIFICATION_ID_BASE = 1000
        
        // SharedPreferences keys
        private const val PREFS_NAME = "member_notification_prefs"
        private const val KEY_LAST_NOTIFICATION_ID = "last_notification_id"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
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
        }
    }
    
    /**
     * Show notification untuk member activity
     */
    fun showNotification(notification: MemberNotification) {
        // Intent untuk membuka app ketika notification di-tap
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_notifications", true) // Extra untuk navigate ke NotificationsScreen
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ganti dengan icon notifikasi kamu
            .setContentTitle("Andalib Library - Aktivitas Anggota")
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID_BASE + notification.id, notificationBuilder.build())
        
        // Save last notification ID
        saveLastNotificationId(notification.id)
    }
    
    /**
     * Show notification dengan title dan message langsung (untuk service)
     */
    fun showNotification(title: String, message: String, notificationId: Int) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // ✅ Full screen intent untuk memaksa heads-up notification
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX) // MAX priority for heads-up
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // Category message
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenIntent, true) // ✅ Force heads-up popup
            .setVibrate(longArrayOf(0, 500, 250, 500)) // Vibration pattern
            .setDefaults(NotificationCompat.DEFAULT_SOUND) // Default notification sound
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lockscreen
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds
        
        notificationManager.notify(NOTIFICATION_ID_BASE + notificationId, notificationBuilder.build())
        saveLastNotificationId(notificationId)
    }
    
    /**
     * Show multiple notifications
     */
    fun showNotifications(notifications: List<MemberNotification>) {
        notifications.forEach { notification ->
            if (!notification.isRead) {
                showNotification(notification)
            }
        }
    }
    
    /**
     * Get last notification ID yang sudah ditampilkan
     */
    fun getLastNotificationId(): Int {
        return prefs.getInt(KEY_LAST_NOTIFICATION_ID, 0)
    }
    
    /**
     * Save last notification ID
     */
    private fun saveLastNotificationId(id: Int) {
        prefs.edit().putInt(KEY_LAST_NOTIFICATION_ID, id).apply()
    }
    
    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * Clear specific notification
     */
    fun clearNotification(notificationId: Int) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + notificationId)
    }
}
