package com.example.travelapp.data.model

import com.google.gson.annotations.SerializedName

data class User(
    // @SerializedName을 사용해 JSON의 'user_id'를 코틀린의 'userId' 변수에 매핑합니다.
    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("email")
    val email: String,

    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("profile_image")
    val profileImage: String?, // 프로필 이미지는 없을 수 있으므로 Nullable '?'

    @SerializedName("social_provider")
    val socialProvider: String?
)

// 소셜 로그인 시 서버로 보낼 데이터 담는 클래스
// @SerializedName, Kotlin 변수 이름과
// 서버가 받는 JSON 데이터 키 이름 다를 때 매핑함.
data class SocialLoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("nickname")
    val nickname: String?,

    @SerializedName("profile_image")
    val profile_image: String?,

    @SerializedName("social_provider")
    val socialProvider: String,

    @SerializedName("social_id")
    val socialId: String
)

// 소셜 로그인 요청 후 받는 응답 데이터 담는 클래스
data class SocialLoginResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("user")
    val user: User
)