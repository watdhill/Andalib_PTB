package com.example.andalib.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.andalib.MainActivity
import com.example.andalib.R
import com.example.andalib.data.TokenManager
import com.example.andalib.data.network.createMemberNotificationService
import com.example.andalib.utils.MemberNotificationHelper
import kotlinx.coroutines.*

/**
 * Foreground Service untuk polling notifications setiap 1 menit
 */
class NotificationPollingService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    
    companion object {
        private const val SERVICE_ID = 1001
        private const val CHANNEL_ID = "notification_polling_service"
        private const val POLLING_INTERVAL_MS = 10_000L // 1 minute
        
        fun start(context: Context) {
            val intent = Intent(context, NotificationPollingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, NotificationPollingService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground dengan persistent notification
        startForeground(SERVICE_ID, createForegroundNotification())
        
        // Start polling
        startPolling()
        
        return START_STICKY // Restart service jika di-kill oleh system
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Service",
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound/vibrate
            ).apply {
                description = "Service untuk mengecek notifikasi anggota"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Layanan Notifikasi Aktif")
            .setContentText("Mendengarkan notifikasi baru (ini bukan notifikasi member)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun startPolling() {
        pollingJob?.cancel() // Cancel existing job jika ada
        
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkNotifications()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Wait 1 minute before next poll
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    private suspend fun checkNotifications() {
        try {
            android.util.Log.d("NotificationService", "🔍 Checking for notifications...")
            
            val tokenManager = TokenManager(this)
            val token = tokenManager.getToken()
            val apiService = createMemberNotificationService(token)
            
            val response = apiService.getUnreadNotifications()
            
            android.util.Log.d("NotificationService", "📡 API Response: success=${response.success}, count=${response.data.size}")
            
            if (response.success && response.data.isNotEmpty()) {
                // Ada notifikasi baru, show notification
                val helper = MemberNotificationHelper(this)
                
                android.util.Log.d("NotificationService", "🔔 Showing ${response.data.size} notifications")
                
                response.data.forEach { notification ->
                    android.util.Log.d("NotificationService", "📬 Notification ID=${notification.id}, Title=${notification.title}")
                    
                    helper.showNotification(
                        title = notification.title ?: "Notifikasi Anggota",
                        message = notification.message,
                        notificationId = notification.id
                    )
                    
                    // ✅ Mark as read setelah ditampilkan (akan auto-delete di backend)
                    try {
                        apiService.markAsRead(notification.id)
                        android.util.Log.d("NotificationService", "✅ Marked notification ${notification.id} as read")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationService", "⚠️ Failed to mark as read: ${e.message}")
                    }
                }
            } else {
                android.util.Log.d("NotificationService", "✅ No new notifications")
            }
        } catch (e: Exception) {
            // Log error untuk debugging
            android.util.Log.e("NotificationService", "❌ Error checking notifications: ${e.message}", e)
            e.printStackTrace()
        }
    }
}
