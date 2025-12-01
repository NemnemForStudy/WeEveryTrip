package com.example.travelapp.data.api

import com.example.travelapp.data.model.LoginRequest
import com.example.travelapp.data.model.SocialLoginRequest
import com.example.travelapp.data.model.SocialLoginResponse
import com.google.android.gms.fido.u2f.api.common.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    // 소셜 로그인(토큰 교환 방식)
    // ⭐️ [수정] 반환 타입을 SocialLoginResponse로 변경 (TokenResponse 없음)
    @POST("api/auth/social-login")
    suspend fun socialLogin(@Body request: SocialLoginRequest): Response<SocialLoginResponse>

    // ⭐️ [수정] 일반 로그인도 SocialLoginResponse 구조를 공유한다고 가정 (서버 응답 형태 확인 필요)
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<SocialLoginResponse>
}