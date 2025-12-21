package com.example.andalib.data.network

import com.example.andalib.screen.pengembalian.ReturnNotifDeleteResponse
import com.example.andalib.screen.pengembalian.ReturnNotifListResponse
import com.example.andalib.screen.pengembalian.ReturnNotifMarkReadResponse
import com.example.andalib.screen.pengembalian.UploadDamageProofResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
// =========================================================
// 1. MODEL PERMINTAAN/RESPONS API (SINKRONISASI)
// =========================================================

data class ReturnRequest(
    val peminjamanId: Int,
    val tanggalPengembalian: String,
    val denda: Int,
    val buktiKerusakanUrl: String? = null,
    val keterangan: String? = null
)

data class PeminjamanResponse(
    val id: Int,
    val judulBuku: String,
    val tglPinjam: String,
    val jatuhTempo: String,
    val author: String
)

data class AnggotaResponse(
    val nim: String,
    val nama: String,
    val email: String?
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

data class ReturnStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: ReturnCreatedResponse? = null
)

data class ReturnCreatedResponse(
    val id: Int,
    val peminjamanId: Int,
    val tanggalPengembalian: String,
    val denda: Int,
    val keterangan: String?,
    val buktiKerusakanUrl: String?
)
typealias SubmitReturnResponse = ReturnStatusResponse

data class UploadProofResponse(
    val success: Boolean,
    val url: String? = null,
    val message: String? = null
)


// =========================================================
// 2. INTERFACE API SERVICE (Retrofit Ready)
// =========================================================

interface ApiService {

    @GET("returns/borrowings/active/{nim}")
    suspend fun fetchActiveLoans(@Path("nim") nim: String): List<PeminjamanResponse>

    @GET("returns/members/search")
    suspend fun searchMembers(@Query("query") query: String): List<AnggotaResponse>

    @GET("returns/history")
    suspend fun getReturnHistory(): List<ReturnHistoryResponse>

    @POST("returns/process")
    suspend fun submitReturn(@Body request: ReturnRequest): ReturnStatusResponse

    @POST("returns/update/{returnId}")
    suspend fun updateReturn(
        @Path("returnId") returnId: String,
        @Body request: ReturnRequest
    ): ReturnStatusResponse

    @DELETE("returns/history/{id}")
    suspend fun deleteReturn(@Path("id") id: Int): ReturnStatusResponse

    // ========= NOTIF API (Masih boleh ada, tapi UI lonceng sudah dihapus) =========
    @GET("returnNotif")
    suspend fun getReturnNotifs(
        @Query("take") take: Int = 50
    ): ReturnNotifListResponse

    @PATCH("returnNotif/{id}/read")
    suspend fun markReturnNotifRead(
        @Path("id") id: Int
    ): ReturnNotifMarkReadResponse

    @DELETE("returnNotif/{id}")
    suspend fun deleteReturnNotif(
        @Path("id") id: Int
    ): ReturnNotifDeleteResponse

    @Multipart
    @POST("returns/process")
    suspend fun submitReturnMultipart(
        @Part("peminjamanId") peminjamanId: okhttp3.RequestBody,
        @Part("tanggalPengembalian") tanggalPengembalian: okhttp3.RequestBody,
        @Part("denda") denda: okhttp3.RequestBody,
        @Part("keterangan") keterangan: okhttp3.RequestBody?,
        @Part buktiKerusakan: okhttp3.MultipartBody.Part?
    ): SubmitReturnResponse

    @Multipart
    @POST("returns/{returnId}/damage-proof")
    suspend fun uploadDamageProof(
        @Path("returnId") returnId: Int,
        @Part buktiKerusakan: okhttp3.MultipartBody.Part
    ): UploadDamageProofResponse


}
