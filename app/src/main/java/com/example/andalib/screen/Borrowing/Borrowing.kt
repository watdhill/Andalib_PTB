package com.example.andalib.screen.Borrowing

import com.google.gson.annotations.SerializedName


data class Borrowing(
    val id: Int = 0,

    @SerializedName("borrowerName")
    val borrowerName: String = "",
    
    val nim: String = "",
    
    val major: String = "",
    
    val contact: String = "",
    

    @SerializedName("bookTitle")
    val bookTitle: String = "",
    
    val author: String = "",

    val stok: Int = 0,
    

    val isbn: String = "",

    @SerializedName("identityPath")
    val identityPath: String = "",

    @SerializedName("borrowDate")
    val borrowDate: String = "",
    
    @SerializedName("returnDate")
    val returnDate: String = "",
    

    val status: String = "DIPINJAM",
    
    // ID buku untuk referensi
    @SerializedName("bukuId")
    val bukuId: Int = 0
)

data class CreateBorrowingRequest(
    val nim: String,
    val bukuId: Int,
    val jatuhTempo: String,
    val tanggalPinjam: String? = null,
    val adminId: Int? = null
)


data class UpdateBorrowingRequest(
    val jatuhTempo: String? = null,
    val status: String? = null
)

data class BorrowingResponse(
    val success: Boolean,
    val data: Borrowing? = null,
    val message: String? = null
)


data class DeleteBorrowingResponse(
    val success: Boolean,
    val message: String? = null
)


data class BukuItem(
    val id: Int,
    val title: String,
    val author: String,
    val stok: Int,
    val isbn: String = "",
    val kategoriId: Int? = null,
    val kategori: KategoriItem? = null
)


data class KategoriItem(
    val id: Int,
    val name: String
)


data class AnggotaItem(
    val nim: String,
    val name: String,
    val major: String? = null,
    val contact: String? = null,
    val email: String? = null
)

data class AnggotaListResponse(
    val success: Boolean,
    val data: List<AnggotaItem>? = null,
    val message: String? = null
)