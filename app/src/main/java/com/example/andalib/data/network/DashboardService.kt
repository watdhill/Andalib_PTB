package com.example.andalib.data.network

import retrofit2.http.GET

/**
 * Data class untuk dashboard statistics
 */
data class DashboardStats(
    val totalBooks: Int,
    val totalMembers: Int,
    val activeBorrowings: Int,
    val overdueBorrowings: Int,
    val totalStock: Int,
    val recentActivities: List<RecentActivity>
)

data class RecentActivity(
    val id: Int,
    val memberName: String,
    val memberNim: String,
    val bookTitle: String,
    val borrowDate: String,
    val dueDate: String,
    val returnDate: String?,
    val status: String
)

data class DashboardResponse(
    val success: Boolean,
    val message: String,
    val data: DashboardStats
)

/**
 * Retrofit Service untuk Dashboard API
 */
interface DashboardService {
    @GET("dashboard/stats")
    suspend fun getDashboardStats(): DashboardResponse
}

/**
 * Factory function untuk create DashboardService dengan auth token
 */
fun createDashboardService(token: String?): DashboardService {
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
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/api/")
        .client(okHttpClient)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    return retrofit.create(DashboardService::class.java)
}
