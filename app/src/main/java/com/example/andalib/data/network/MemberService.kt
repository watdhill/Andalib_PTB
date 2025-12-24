package com.example.andalib.data.network

import com.example.andalib.screen.member.MemberActionResponse
import com.example.andalib.screen.member.MemberDetailResponse
import com.example.andalib.screen.member.MemberListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface MemberService {

    @GET("anggota")
    suspend fun getAllMembers(): MemberListResponse

    @GET("anggota/{nim}")
    suspend fun getMemberDetail(@Path("nim") nim: String): MemberDetailResponse

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


    @Multipart
    @PUT("anggota/{targetNim}")
    suspend fun updateMember(
        @Path("targetNim") targetNim: String,
        @Part("name") name: RequestBody,    
        @Part("gender") gender: RequestBody,
        @Part("faculty") faculty: RequestBody,
        @Part("major") major: RequestBody,
        @Part("contact") contact: RequestBody,
        @Part("email") email: RequestBody,
        @Part photo: MultipartBody.Part?
    ): MemberActionResponse


    @DELETE("anggota/{nim}")
    suspend fun deleteMember(@Path("nim") nim: String): MemberActionResponse
    

    @Multipart
    @POST("anggota/{id}/photo")
    suspend fun uploadPhoto(
        @Path("id") memberId: Int,
        @Part photo: MultipartBody.Part
    ): MemberActionResponse
}