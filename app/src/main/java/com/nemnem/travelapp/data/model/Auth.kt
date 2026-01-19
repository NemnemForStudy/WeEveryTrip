package com.nemnem.travelapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * 소셜 로그인 요청 데이터
 * 보안을 위해 사용자의 이메일/닉네임을 직접 보내지 않고,
 * 소셜 플랫폼(카카오/네이버)에서 발급받은 '토큰'을 서버로 전송합니다.
 * 서버가 이 토큰으로 직접 유효성을 검증합니다.
 */
data class SocialLoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("social_provider")
    val socialProvider: String,

    @SerializedName("social_id")
    val socialId: String,

    @SerializedName("nickname")
    val nickname: String? = null,
    val profileImage: String? = null
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

// 소셜 로그인 요청 후 받는 응답 데이터 담는 클래스
data class SocialLoginResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("token") // 백엔드에서 "token"으로 보내면 그대로 둡니다.
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("user")
    val user: User
)