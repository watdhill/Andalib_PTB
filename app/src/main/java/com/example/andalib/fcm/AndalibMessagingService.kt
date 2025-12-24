package com.example.andalib.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.andalib.MainActivity
import com.example.andalib.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AndalibMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AndalibFCM"

        // Samakan dengan topic backend Anda
        const val ADMIN_TOPIC = "andalib-admin"

        private const val CHANNEL_ID = "andalib_fcm_channel"
        private const val CHANNEL_NAME = "Andalib Notifications"
        private const val CHANNEL_DESC = "Notifikasi Andalib (FCM)"

        /**
         * Panggil ini setelah admin berhasil login / saat app start untuk admin device.
         * Ini WAJIB kalau backend mengirim ke "topic".
         */
        fun subscribeAdminTopic() {
            FirebaseMessaging.getInstance().subscribeToTopic(ADMIN_TOPIC)
                .addOnSuccessListener { Log.d(TAG, "Subscribed to topic: $ADMIN_TOPIC") }
                .addOnFailureListener { e -> Log.e(TAG, "Subscribe topic failed: ${e.message}", e) }
        }

        /**
         * Untuk debugging: lihat token device di Logcat.
         * Jika ingin kirim via token (bukan topic), token ini yang dipakai.
         */
        fun logCurrentToken() {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> Log.d(TAG, "FCM token: $token") }
                .addOnFailureListener { e -> Log.e(TAG, "Get token failed: ${e.message}", e) }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")

        // Jika nanti Anda ingin kirim by token (bukan topic),
        // kirim token ini ke backend dan simpan per user/admin.
        // TODO: call API backend: /api/device-token (authorized).
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Log lengkap agar Anda tahu apakah device menerima pesan atau tidak
        Log.d(TAG, "onMessageReceived from: ${message.from}")
        Log.d(TAG, "data: ${message.data}")
        Log.d(TAG, "notification: title=${message.notification?.title}, body=${message.notification?.body}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Andalib"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: ""

        // Optional: Anda bisa baca tipe notifikasi dari data payload
        val type = message.data["type"] ?: "UNKNOWN"
        val peminjamanId = message.data["peminjamanId"] ?: ""
        val pengembalianId = message.data["pengembalianId"] ?: ""

        showNotification(
            title = title,
            body = body,
            extras = mapOf(
                "type" to type,
                "peminjamanId" to peminjamanId,
                "pengembalianId" to pengembalianId
            )
        )
    }

    private fun showNotification(title: String, body: String, extras: Map<String, String> = emptyMap()) {
        createChannelIfNeeded()

        // Android 13+ wajib cek permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted, notification will not be shown.")
                return
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // lempar data agar bisa diarahkan ke halaman tertentu kalau mau
            extras.forEach { (k, v) -> putExtra(k, v) }
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Anda bisa ganti icon notifikasi khusus
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        NotificationManagerCompat.from(this).notify(notifId, notification)

        Log.d(TAG, "Notification shown. id=$notifId title=$title")
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }

            nm.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
}