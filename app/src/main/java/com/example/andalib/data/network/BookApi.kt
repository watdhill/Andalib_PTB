package com.example.andalib.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.Response

private const val BASE_URL = "http://192.168.1.20:3000/api/"

fun createSimpleRetrofit(): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

interface BookService {
    @POST("buku")
    suspend fun createBook(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any>>

    @PUT("buku/{id}")
    suspend fun updateBook(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Map<String, Any>>

    @DELETE("buku/{id}")
    suspend fun deleteBook(@Path("id") id: Int): Response<Void>
}

fun createBookService(): BookService = createSimpleRetrofit().create(BookService::class.java)
