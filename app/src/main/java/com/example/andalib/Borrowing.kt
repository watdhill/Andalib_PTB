package com.example.andalib

import com.google.gson.annotations.SerializedName

/**
 * Data class untuk response peminjaman dari backend.
 * Sesuai dengan format response dari GET /api/peminjaman
 */
data class Borrowing(
    val id: Int = 0,
    
    // Data Peminjam (dari relasi Anggota)
    @SerializedName("borrowerName")
    val borrowerName: String = "",
    
    val nim: String = "",
    
    val major: String = "",
    
    val contact: String = "",
    
    // Data Buku (dari relasi Buku)
    @SerializedName("bookTitle")
    val bookTitle: String = "",
    
    val author: String = "",
    
    // Stok buku dari relasi Buku
    val stok: Int = 0,
    
    // ISBN tidak ada di DB, tetap ada untuk kompatibilitas UI
    val isbn: String = "",
    
    // Path foto KRS (opsional)
    @SerializedName("identityPath")
    val identityPath: String = "",
    
    // Tanggal dalam format dd/MM/yyyy
    @SerializedName("borrowDate")
    val borrowDate: String = "",
    
    @SerializedName("returnDate")
    val returnDate: String = "",
    
    // Status peminjaman: DIPINJAM atau DIKEMBALIKAN
    val status: String = "DIPINJAM",
    
    // ID buku untuk referensi
    @SerializedName("bukuId")
    val bukuId: Int = 0
)

/**
 * Request body untuk create peminjaman
 * Digunakan saat POST /api/peminjaman
 */
data class CreateBorrowingRequest(
    val nim: String,
    val bukuId: Int,
    val jatuhTempo: String,      // Format: dd/MM/yyyy
    val tanggalPinjam: String? = null,  // Opsional, default: sekarang
    val adminId: Int? = null     // Opsional
)

/**
 * Request body untuk update peminjaman
 * Digunakan saat PUT /api/peminjaman/:id
 */
data class UpdateBorrowingRequest(
    val jatuhTempo: String? = null,  // Format: dd/MM/yyyy
    val status: String? = null       // DIPINJAM atau DIKEMBALIKAN
)

/**
 * Response wrapper dari backend
 */
data class BorrowingResponse(
    val success: Boolean,
    val data: Borrowing? = null,
    val message: String? = null
)

/**
 * Response untuk delete
 */
data class DeleteBorrowingResponse(
    val success: Boolean,
    val message: String? = null
)

/**
 * Data class untuk daftar buku (dropdown pemilihan buku)
 */
data class BukuItem(
    val id: Int,
    val title: String,
    val author: String,
    val stok: Int,
    val kategoriId: Int? = null,
    val kategori: KategoriItem? = null
)

/**
 * Data class untuk kategori buku
 */
data class KategoriItem(
    val id: Int,
    val name: String
)

/**
 * Data class untuk daftar anggota (dropdown pemilihan anggota)
 */
data class AnggotaItem(
    val nim: String,
    val name: String,
    val major: String? = null,
    val contact: String? = null,
    val email: String? = null
)

/**
 * Response wrapper untuk list anggota dari GET /api/anggota
 */
data class AnggotaListResponse(
    val success: Boolean,
    val data: List<AnggotaItem>? = null,
    val message: String? = null
)