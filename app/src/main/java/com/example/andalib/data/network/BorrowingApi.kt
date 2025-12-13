package com.example.andalib.data.network

import com.example.andalib.AnggotaItem
import com.example.andalib.AnggotaListResponse
import com.example.andalib.Borrowing
import com.example.andalib.BorrowingResponse
import com.example.andalib.BukuItem
import com.example.andalib.CreateBorrowingRequest
import com.example.andalib.DeleteBorrowingResponse
import com.example.andalib.UpdateBorrowingRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface API untuk modul Peminjaman
 * Base URL: /api/peminjaman
 */
interface BorrowingApi {

    // =========================================================
    // PEMINJAMAN ENDPOINTS
    // =========================================================

    /**
     * Mengambil semua data peminjaman
     * GET /api/peminjaman
     */
    @GET("peminjaman")
    suspend fun getAllBorrowings(): Response<List<Borrowing>>

    /**
     * Mencari peminjaman berdasarkan query
     * GET /api/peminjaman/search?q=...
     */
    @GET("peminjaman/search")
    suspend fun searchBorrowings(@Query("q") query: String): Response<List<Borrowing>>

    /**
     * Mengambil peminjaman berdasarkan ID
     * GET /api/peminjaman/:id
     */
    @GET("peminjaman/{id}")
    suspend fun getBorrowingById(@Path("id") id: Int): Response<Borrowing>

    /**
     * Mengambil peminjaman aktif berdasarkan NIM anggota
     * GET /api/peminjaman/active/:nim
     */
    @GET("peminjaman/active/{nim}")
    suspend fun getActiveBorrowingsByNim(@Path("nim") nim: String): Response<List<Borrowing>>

    /**
     * Membuat peminjaman baru (tanpa upload KRS)
     * POST /api/peminjaman
     */
    @POST("peminjaman")
    suspend fun createBorrowing(@Body request: CreateBorrowingRequest): Response<BorrowingResponse>

    /**
     * Membuat peminjaman baru dengan upload KRS (multipart)
     * POST /api/peminjaman
     */
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

    /**
     * Update peminjaman
     * PUT /api/peminjaman/:id
     */
    @PUT("peminjaman/{id}")
    suspend fun updateBorrowing(
        @Path("id") id: Int,
        @Body request: UpdateBorrowingRequest
    ): Response<BorrowingResponse>

    /**
     * Update peminjaman dengan upload KRS baru (multipart)
     * PUT /api/peminjaman/:id
     */
    @Multipart
    @PUT("peminjaman/{id}")
    suspend fun updateBorrowingWithKrs(
        @Path("id") id: Int,
        @Part("jatuhTempo") jatuhTempo: RequestBody?,
        @Part("status") status: RequestBody?,
        @Part krsImage: MultipartBody.Part?
    ): Response<BorrowingResponse>

    /**
     * Upload KRS saja untuk peminjaman yang sudah ada
     * POST /api/peminjaman/:id/upload-krs
     */
    @Multipart
    @POST("peminjaman/{id}/upload-krs")
    suspend fun uploadKrs(
        @Path("id") id: Int,
        @Part krsImage: MultipartBody.Part
    ): Response<BorrowingResponse>

    /**
     * Hapus peminjaman
     * DELETE /api/peminjaman/:id
     */
    @DELETE("peminjaman/{id}")
    suspend fun deleteBorrowing(@Path("id") id: Int): Response<DeleteBorrowingResponse>

    // =========================================================
    // HELPER ENDPOINTS (untuk dropdown)
    // =========================================================

    /**
     * Mengambil semua buku untuk dropdown
     * GET /api/buku
     */
    @GET("buku")
    suspend fun getAllBooks(): Response<List<BukuItem>>

    /**
     * Mencari buku berdasarkan query
     * GET /api/buku/search?q=...
     */
    @GET("buku/search")
    suspend fun searchBooks(@Query("q") query: String): Response<List<BukuItem>>

    /**
     * Mengambil semua anggota (wrapper response dengan success dan data)
     * GET /api/anggota
     */
    @GET("anggota")
    suspend fun getAllMembers(): Response<AnggotaListResponse>

    /**
     * Mencari anggota berdasarkan query (langsung return list)
     * GET /api/anggota/search?q=...
     */
    @GET("anggota/search")
    suspend fun searchMembers(@Query("q") query: String): Response<List<AnggotaItem>>
}