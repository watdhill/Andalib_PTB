package com.example.andalib.data.network

import com.example.andalib.Borrowing
import retrofit2.Response // PENTING: Menggunakan Response
import retrofit2.http.*

interface BorrowingApi {

    @GET("peminjaman")
    suspend fun getAllBorrowings(): Response<List<Borrowing>> // GANTI ke Response<List<Borrowing>>

    @GET("peminjaman/search")
    suspend fun searchBorrowings(@Query("q") query: String): Response<List<Borrowing>> // GANTI ke Response<List<Borrowing>>

    @POST("peminjaman")
    suspend fun createBorrowing(@Body borrowing: Borrowing): Response<Borrowing> // GANTI ke Response<Borrowing>

    @PUT("peminjaman/{id}")
    suspend fun updateBorrowing(@Path("id") id: Int, @Body borrowing: Borrowing): Response<Borrowing> // GANTI ke Response<Borrowing>

    @DELETE("peminjaman/{id}")
    suspend fun deleteBorrowing(@Path("id") id: Int): Response<Unit> // GANTI ke Response<Unit>
}