package com.example.andalib.screen.member


import com.google.gson.annotations.SerializedName

data class MemberApi(
    @SerializedName("nim")
    val nim: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("gender")
    val gender: String, 

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


data class MemberListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<MemberApi>
)


data class MemberDetailResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: MemberApi
)

data class MemberActionResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: MemberApi? = null 
)