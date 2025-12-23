package com.example.andalib.data.network

import com.example.andalib.screen.Borrowing.AnggotaItem
import com.example.andalib.screen.Borrowing.AnggotaListResponse
import com.example.andalib.screen.Borrowing.Borrowing
import com.example.andalib.screen.Borrowing.BorrowingResponse
import com.example.andalib.screen.Borrowing.BukuItem
import com.example.andalib.screen.Borrowing.CreateBorrowingRequest
import com.example.andalib.screen.Borrowing.DeleteBorrowingResponse
import com.example.andalib.screen.Borrowing.UpdateBorrowingRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*


interface BorrowingApi {

    @GET("peminjaman")
    suspend fun getAllBorrowings(): Response<List<Borrowing>>


    @GET("peminjaman/search")
    suspend fun searchBorrowings(@Query("q") query: String): Response<List<Borrowing>>


    @GET("peminjaman/{id}")
    suspend fun getBorrowingById(@Path("id") id: Int): Response<Borrowing>


    @GET("peminjaman/active/{nim}")
    suspend fun getActiveBorrowingsByNim(@Path("nim") nim: String): Response<List<Borrowing>>


    @POST("peminjaman")
    suspend fun createBorrowing(@Body request: CreateBorrowingRequest): Response<BorrowingResponse>


    @Multipart
    @POST("peminjaman")
    suspend fun createBorrowingWithKrs(
        @Part("nim") nim: RequestBody,
        @Part("bukuId") bukuId: RequestBody,
        @Part("jatuhTempo") jatuhTempo: RequestBody,
        @Part("tanggalPinjam") tanggalPinjam: RequestBody?,
        @Part("adminId") adminId: RequestBody?,
        @Part krsImage: MultipartBody.Part?
    ): Response<BorrowingResponse>


    @PUT("peminjaman/{id}")
    suspend fun updateBorrowing(
        @Path("id") id: Int,
        @Body request: UpdateBorrowingRequest
    ): Response<BorrowingResponse>


    @Multipart
    @PUT("peminjaman/{id}")
    suspend fun updateBorrowingWithKrs(
        @Path("id") id: Int,
        @Part("jatuhTempo") jatuhTempo: RequestBody?,
        @Part("status") status: RequestBody?,
        @Part krsImage: MultipartBody.Part?
    ): Response<BorrowingResponse>


    @Multipart
    @POST("peminjaman/{id}/upload-krs")
    suspend fun uploadKrs(
        @Path("id") id: Int,
        @Part krsImage: MultipartBody.Part
    ): Response<BorrowingResponse>


    @DELETE("peminjaman/{id}")
    suspend fun deleteBorrowing(@Path("id") id: Int): Response<DeleteBorrowingResponse>

    // =========================================================
    // HELPER ENDPOINTS (untuk dropdown)
    // =========================================================


    @GET("buku")
    suspend fun getAllBooks(): Response<List<BukuItem>>


    @GET("buku/search")
    suspend fun searchBooks(@Query("q") query: String): Response<List<BukuItem>>

    @GET("anggota")
    suspend fun getAllMembers(): Response<AnggotaListResponse>


    @GET("anggota/search")
    suspend fun searchMembers(@Query("q") query: String): Response<List<AnggotaItem>>
}