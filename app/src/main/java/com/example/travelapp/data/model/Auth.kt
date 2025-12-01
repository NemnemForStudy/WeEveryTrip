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

/**
 * 소셜 로그인 요청 데이터
 * 보안을 위해 사용자의 이메일/닉네임을 직접 보내지 않고,
 * 소셜 플랫폼(카카오/네이버)에서 발급받은 '토큰'을 서버로 전송합니다.
 * 서버가 이 토큰으로 직접 유효성을 검증합니다.
 */
data class SocialLoginRequest(
    // 보안 문제로 인해 provider, token만 보내는걸로 변경.
    @SerializedName("provider")
    val provider: String,

    @SerializedName("token")
    val token: String
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

    @SerializedName("token")
    val token: String,

    @SerializedName("user")
    val user: User
)