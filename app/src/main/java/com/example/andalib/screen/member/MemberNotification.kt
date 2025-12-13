package com.example.andalib.screen.member

import com.google.gson.annotations.SerializedName

/**
 * Data class untuk Member Notification
 */
data class MemberNotification(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("admin_id")
    val adminId: Int,
    
    @SerializedName("notification_type")
    val notificationType: String,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("member_name")
    val memberName: String? = null,
    
    @SerializedName("member_nim")
    val memberNim: String? = null,
    
    @SerializedName("deleted_by_admin_name")
    val deletedByAdminName: String? = null,
    
    @SerializedName("is_read")
    val isRead: Boolean = false,
    
    @SerializedName("created_at")
    val created_at: String
)

/**
 * Response untuk API notifications
 */
data class MemberNotificationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<MemberNotification> = emptyList()
)

/**
 * Response untuk count unread notifications
 */
data class NotificationCountResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("count")
    val count: Int = 0
)

/**
 * Generic API response
 */
data class ApiResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String
)

/**
 * Helper function untuk format relative time
 */
fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(isoTimestamp)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        
        when {
            duration.toMinutes() < 1 -> "Baru saja"
            duration.toMinutes() < 60 -> "${duration.toMinutes()} menit yang lalu"
            duration.toHours() < 24 -> "${duration.toHours()} jam yang lalu"
            duration.toDays() < 7 -> "${duration.toDays()} hari yang lalu"
            duration.toDays() < 30 -> "${duration.toDays() / 7} minggu yang lalu"
            else -> "${duration.toDays() / 30} bulan yang lalu"
        }
    } catch (e: Exception) {
        isoTimestamp
    }
}
