package com.example.andalib.data.network

import com.example.andalib.screen.member.ApiResponse
import com.example.andalib.screen.member.MemberNotificationResponse
import com.example.andalib.screen.member.NotificationCountResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Request body untuk create notification
 */
data class CreateNotificationRequest(
    val type: String,
    val title: String,
    val message: String,
    val bookTitle: String? = null,
    val bookIsbn: String? = null
)

/**
 * Retrofit Service untuk Member Notifications API
 */
interface MemberNotificationService {

    /**
     * Get unread notifications untuk current admin
     */
    @GET("member-notifications/unread")
    suspend fun getUnreadNotifications(): MemberNotificationResponse

    /**
     * Get all notifications (read + unread)
     */
    @GET("member-notifications")
    suspend fun getAllNotifications(): MemberNotificationResponse

    /**
     * Mark notification as read
     */
    @PUT("member-notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Int): ApiResponse

    /**
     * Get count of unread notifications
     */
    @GET("member-notifications/count")
    suspend fun getUnreadCount(): NotificationCountResponse
    
    /**
     * Create new notification (for book activities)
     */
    @POST("member-notifications")
    suspend fun createNotification(@Body request: CreateNotificationRequest): ApiResponse
}

/**
 * Factory function untuk create MemberNotificationService dengan auth token
 */
fun createMemberNotificationService(token: String?): MemberNotificationService {
    val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            token?.let {
                if (it.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $it")
                }
            }
            chain.proceed(requestBuilder.build())
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS) // ✅ Timeout 15 detik
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)    // ✅ Timeout 15 detik
        .build()

    val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/api/") // Ganti dengan base URL backend kamu
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    return retrofit.create(MemberNotificationService::class.java)
}
