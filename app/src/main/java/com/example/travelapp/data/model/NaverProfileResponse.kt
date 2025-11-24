package com.example.travelapp.data.model

import com.google.gson.annotations.SerializedName
import javax.annotation.meta.TypeQualifierNickname

data class NaverProfileResponse(
    @SerializedName("resultCode")
    val resultCode: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("response")
    val response: NaverProfile
)

data class NaverProfile(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("nickname")
    val nickname: String?,
    @SerializedName("profile_image")
    val profileImage: String?
)
