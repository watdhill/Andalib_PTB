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
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// PASTIKAN URL INI BENAR (Emulator ke port 3000 Express.js)
private const val BASE_URL = "http://192.168.1.9:3000/api/"

fun createAuthInterceptor(tokenManager: TokenManager): Interceptor {
    return Interceptor { chain ->
        val token = runBlocking {
            tokenManager.authToken.first()
        }

        val requestBuilder = chain.request().newBuilder()

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }
}

fun createHttpClient(tokenManager: TokenManager): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    return OkHttpClient.Builder()
        .addInterceptor(createAuthInterceptor(tokenManager))
        .addInterceptor(logging)
        .build()
}

fun createRetrofit(tokenManager: TokenManager): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createHttpClient(tokenManager))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

interface AuthService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // Endpoint Sign Up (TAMBAH INI)
    @POST("signup")
    suspend fun signup(@Body request: SignUpRequest): SignUpResponse

}

fun createAuthService(tokenManager: TokenManager): AuthService {
    return createRetrofit(tokenManager).create(AuthService::class.java)
}