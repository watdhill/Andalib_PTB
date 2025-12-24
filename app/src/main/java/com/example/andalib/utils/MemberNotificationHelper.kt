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

class MemberNotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "member_notifications_channel"
        private const val CHANNEL_NAME = "Member Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for member management activities"
        private const val NOTIFICATION_ID_BASE = 1000
        
        private const val PREFS_NAME = "member_notification_prefs"
        private const val KEY_LAST_NOTIFICATION_ID = "last_notification_id"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        createNotificationChannel()
    }
    
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
    
  
    fun showNotification(notification: MemberNotification) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_notifications", true)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ganti dengan icon notifikasi kamu
            .setContentTitle("Andalib Library - Aktivitas Anggota")
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))
        
        notificationManager.notify(NOTIFICATION_ID_BASE + notification.id, notificationBuilder.build())
        
        saveLastNotificationId(notification.id)
    }
    
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
            .setPriority(NotificationCompat.PRIORITY_MAX) 
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) 
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenIntent, true) 
            .setVibrate(longArrayOf(0, 500, 250, 500)) 
            .setDefaults(NotificationCompat.DEFAULT_SOUND) 
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) 
            .setTimeoutAfter(5000) 
        
        notificationManager.notify(NOTIFICATION_ID_BASE + notificationId, notificationBuilder.build())
        saveLastNotificationId(notificationId)
    }
    

    fun showNotifications(notifications: List<MemberNotification>) {
        notifications.forEach { notification ->
            if (!notification.isRead) {
                showNotification(notification)
            }
        }
    }
    

    fun getLastNotificationId(): Int {
        return prefs.getInt(KEY_LAST_NOTIFICATION_ID, 0)
    }
    
  
    private fun saveLastNotificationId(id: Int) {
        prefs.edit().putInt(KEY_LAST_NOTIFICATION_ID, id).apply()
    }
    

    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
    

    fun clearNotification(notificationId: Int) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + notificationId)
    }
}
