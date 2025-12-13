package com.example.andalib.data.network

import com.example.andalib.data.login.LoginRequest
import com.example.andalib.data.login.LoginResponse
import com.example.andalib.data.signup.SignUpRequest
import com.example.andalib.data.signup.SignUpResponse
import com.example.andalib.data.TokenManager // Pastikan TokenManager ada di package ini
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


// =====================================================================
// KONSTANTA
// =====================================================================

// !!! GANTI BASE_URL INI DENGAN ALAMAT IP SERVER BACKEND ANDA !!!
// Contoh: "http://10.0.2.2:3000/api/" jika menggunakan emulator Android dan server lokal
private const val BASE_URL = "http://10.0.2.2:3000/api/"

// =====================================================================
// INTERCEPTOR & HTTP CLIENT
// =====================================================================

/**
 * Interceptor untuk menambahkan token JWT ke setiap request terproteksi.
 */
fun createAuthInterceptor(tokenManager: TokenManager): Interceptor {
    return Interceptor { chain ->
        val token = runBlocking {
            tokenManager.authToken.first()
        }

        val requestBuilder = chain.request().newBuilder()

        // Tambahkan header Authorization jika token tersedia
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }
}

/**
 * Konfigurasi OkHttpClient dengan logging dan auth interceptor.
 */
fun createHttpClient(tokenManager: TokenManager): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
        // Level BODY menampilkan request dan response body di Logcat
        level = HttpLoggingInterceptor.Level.BODY
    }

    return OkHttpClient.Builder()
        .addInterceptor(createAuthInterceptor(tokenManager))
        .addInterceptor(logging)
        .build()
}

// =====================================================================
// RETROFIT INSTANCE FACTORY
// =====================================================================

/**
 * Inisialisasi Retrofit.
 */
fun createRetrofit(tokenManager: TokenManager): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createHttpClient(tokenManager))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

// =====================================================================
// INTERFACE SERVICES
// =====================================================================

/**
 * Interface Service API untuk otentikasi.
 */
interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun signup(@Body request: SignUpRequest): Response<SignUpResponse>
}


// =====================================================================
// SERVICE FACTORY FUNCTIONS
// =====================================================================

/**
 * Fungsi untuk membuat instance AuthService.
 */
fun createAuthService(tokenManager: TokenManager): AuthService {
    return createRetrofit(tokenManager).create(AuthService::class.java)
}

/**
 * BARU: Fungsi untuk membuat instance ApiService (untuk modul Pengembalian).
 */
fun createApiService(tokenManager: TokenManager): ApiService {
    return createRetrofit(tokenManager).create(ApiService::class.java)
}

/**
 * Fungsi untuk membuat instance BorrowingApi.
 */
fun createBorrowingService(tokenManager: TokenManager): BorrowingApi {
    return createRetrofit(tokenManager).create(BorrowingApi::class.java)
}

/**
 * Fungsi untuk membuat instance MemberService.
 */
fun createMemberService(tokenManager: TokenManager): MemberService {
    return createRetrofit(tokenManager).create(MemberService::class.java)
}