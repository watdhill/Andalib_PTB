package com.example.andalib.data.network

import com.example.andalib.screen.member.ApiResponse
import com.example.andalib.screen.member.MemberNotificationResponse
import com.example.andalib.screen.member.NotificationCountResponse
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path


interface MemberNotificationService {
    

    @GET("member-notifications/unread")
    suspend fun getUnreadNotifications(): MemberNotificationResponse
    

    @GET("member-notifications")
    suspend fun getAllNotifications(): MemberNotificationResponse
    

    @PUT("member-notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Int): ApiResponse
    

    @GET("member-notifications/count")
    suspend fun getUnreadCount(): NotificationCountResponse
}


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
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS) 
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)  
        .build()

    val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/api/") 
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    return retrofit.create(MemberNotificationService::class.java)
}
