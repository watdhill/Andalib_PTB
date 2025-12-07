package com.example.andalib.data.network

import com.example.andalib.data.login.LoginRequest
import com.example.andalib.data.login.LoginResponse
import com.example.andalib.data.signup.SignUpRequest
import com.example.andalib.data.signup.SignUpResponse
import com.example.andalib.data.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.Response // PENTING: Untuk menangani status HTTP
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// PASTIKAN URL INI BENAR! Ganti jika IP PC Anda berubah atau jika Anda menggunakan emulator standar (10.0.2.2).
private const val BASE_URL = "http://192.168.1.10:3000/api/"

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

/**
 * Interface Service API untuk otentikasi.
 */
interface AuthService {
    // KITAUBAH JADI: Response<LoginResponse>
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // KITAUBAH JADI: Response<SignUpResponse>
    // Ini memperbaiki error "Unresolved reference" di ViewModel nanti karena kita akan akses body()
    @POST("auth/register") // Pastikan ini cocok dengan /api/auth + /register
    suspend fun signup(@Body request: SignUpRequest): Response<SignUpResponse>
}

/**
 * Fungsi untuk membuat instance AuthService.
 */
fun createAuthService(tokenManager: TokenManager): AuthService {
    return createRetrofit(tokenManager).create(AuthService::class.java)
}

// =====================================================================
// BARU: Fungsi untuk membuat instance BorrowingApi.
// (Interface BorrowingApi ASLI seharusnya ada di file BorrowingApi.kt)
// =====================================================================
fun createBorrowingService(tokenManager: TokenManager): BorrowingApi {
    return createRetrofit(tokenManager).create(BorrowingApi::class.java)
}

/**
 * Fungsi untuk membuat instance MemberService.
 */
fun createMemberService(tokenManager: TokenManager): MemberService {
    return createRetrofit(tokenManager).create(MemberService::class.java)
}