package com.example.andalib.screen.member

import com.google.gson.annotations.SerializedName


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


data class MemberNotificationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<MemberNotification> = emptyList()
)


data class NotificationCountResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("count")
    val count: Int = 0
)


data class ApiResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String
)

