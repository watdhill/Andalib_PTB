package com.example.andalib.data.network

import com.example.andalib.screen.member.MemberActionResponse
import com.example.andalib.screen.member.MemberDetailResponse
import com.example.andalib.screen.member.MemberListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface MemberService {

    // Lihat Daftar Anggota
    @GET("anggota")
    suspend fun getAllMembers(): MemberListResponse

    // Lihat Detail Anggota
    @GET("anggota/{nim}")
    suspend fun getMemberDetail(@Path("nim") nim: String): MemberDetailResponse

    // Tambah Anggota (Multipart)
    @Multipart
    @POST("anggota")
    suspend fun createMember(
        @Part("nim") nim: RequestBody,
        @Part("name") name: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("faculty") faculty: RequestBody,
        @Part("major") major: RequestBody,
        @Part("contact") contact: RequestBody,
        @Part("email") email: RequestBody,
        @Part photo: MultipartBody.Part?
    ): MemberActionResponse

    // Edit Anggota (Multipart)
    @Multipart
    @PUT("anggota/{targetNim}")
    suspend fun updateMember(
        @Path("targetNim") targetNim: String,
        @Part("name") name: RequestBody,
        // NIM biasanya tidak diedit jika jadi PK, tapi jika backend mengizinkan, kirim di body
        // @Part("nim") nim: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("faculty") faculty: RequestBody,
        @Part("major") major: RequestBody,
        @Part("contact") contact: RequestBody,
        @Part("email") email: RequestBody,
        @Part photo: MultipartBody.Part?
    ): MemberActionResponse

    // Hapus Anggota
    @DELETE("anggota/{nim}")
    suspend fun deleteMember(@Path("nim") nim: String): MemberActionResponse
    
    // Upload Foto Anggota (untuk update foto existing member)
    @Multipart
    @POST("anggota/{id}/photo")
    suspend fun uploadPhoto(
        @Path("id") memberId: Int,
        @Part photo: MultipartBody.Part
    ): MemberActionResponse
}