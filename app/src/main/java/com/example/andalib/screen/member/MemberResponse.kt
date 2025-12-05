package com.example.andalib.screen.member


import com.google.gson.annotations.SerializedName

// Model Data Anggota dari API (sesuai dengan respon backend Prisma/Nodejs)
data class MemberApi(
    @SerializedName("nim")
    val nim: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("gender")
    val gender: String, // Backend mengirim String (Enum value)

    @SerializedName("faculty")
    val faculty: String,

    @SerializedName("major")
    val major: String,

    @SerializedName("contact")
    val contact: String,

    @SerializedName("email")
    val email: String?,

    @SerializedName("photoUrl")
    val photoUrl: String? = null,

    @SerializedName("registrationDate")
    val registrationDate: String? = null
)

// 1. Respon untuk Lihat Daftar Anggota (GET All)
data class MemberListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<MemberApi>
)

// 2. Respon untuk Detail Anggota (GET by NIM)
data class MemberDetailResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: MemberApi
)

// 3. Respon untuk Aksi (Tambah, Edit, Hapus)
data class MemberActionResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: MemberApi? = null // Opsional, kadang backend mengembalikan data yang baru dibuat/diedit
)