package com.example.andalib.data.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.DELETE

// =========================================================
// 1. MODEL PERMINTAAN/RESPONS API (SINKRONISASI)
// =========================================================

// Model data yang akan dikirim saat submit pengembalian
data class ReturnRequest(
    // Perbaikan: Sesuaikan dengan field backend
    val peminjamanId: Int,
    val tanggalPengembalian: String, // Tanggal dalam format String (backend akan konversi)
    val denda: Int, // Sesuai nama field di backend
    val buktiKerusakanUrl: String? = null, // Opsional
    val keterangan: String? = null // Opsional
)

// Model respons untuk daftar pinjaman
data class PeminjamanResponse(
    // Sinkron: Id pinjaman dari tabel Peminjaman
    val id: Int,
    // Sinkron: Judul buku dari tabel Buku
    val judulBuku: String,
    // Sinkron: Tanggal yang sudah diformat dari backend
    val tglPinjam: String,
    val jatuhTempo: String,
    // Tambahan yang dikirim dari backend
    val author: String
)

// Model respons untuk detail anggota
data class AnggotaResponse(
    // Sinkron: Sesuai hasil mapping di returnController.js
    val nim: String,
    val nama: String, // Menggunakan 'nama' dari hasil mapping controller
    val email: String? // Tambahkan email jika diperlukan
)

// Model respons status sederhana - TIDAK BERUBAH
data class ReturnStatusResponse(
    val success: Boolean,
    val message: String? = null
)
data class ReturnHistoryResponse(
    val id: Int,
    val peminjamanId: Int,
    val judulBuku: String,
    val namaAnggota: String,
    val nimAnggota: String,
    val tanggalPinjam: String,
    val jatuhTempo: String,
    val tanggalPengembalian: String,
    val denda: Int,
    val keterangan: String?,
    val buktiKerusakanUrl: String?
)

data class SimpleResponse(
    val success: Boolean,
    val message: String?
)

// =========================================================
// 2. INTERFACE API SERVICE (Retrofit Ready)
// =========================================================

/**
 * Interface ini mendefinisikan method yang berinteraksi dengan API server.
 */
interface ApiService {
    /**
     * Mengambil daftar pinjaman aktif dari seorang anggota.
     * Endpoint: GET /borrowings/active/{nim}
     */
    @GET("returns/borrowings/active/{nim}")
    suspend fun fetchActiveLoans(@Path("nim") nim: String): List<PeminjamanResponse>

    /**
     * Endpoint untuk mencari anggota berdasarkan query.
     * Endpoint: GET /members/search?query={query}
     */
    @GET("returns/members/search")
    suspend fun searchMembers(@Query("query") query: String): List<AnggotaResponse>

    @GET("returns/history")
    suspend fun getReturnHistory(): List<ReturnHistoryResponse>
    /**
     * Mengirim data pengembalian ke server untuk diproses.
     * Endpoint: POST /process
     */
    @POST("returns/process")
    suspend fun submitReturn(@Body request: ReturnRequest): ReturnStatusResponse

    /**
     * Endpoint untuk mengupdate data pengembalian yang sudah ada.
     * Endpoint: POST /update/{returnId}
     */
    @POST("returns/update/{returnId}")
    suspend fun updateReturn(@Path("returnId") returnId: String, @Body request: ReturnRequest): ReturnStatusResponse

    @DELETE("returns/history/{id}")
    suspend fun deleteReturn(@Path("id") id: Int): SimpleResponse
}