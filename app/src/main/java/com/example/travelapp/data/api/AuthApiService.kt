package com.example.travelapp.data.api

import com.example.travelapp.data.model.ApiResponse
import com.example.travelapp.data.model.LoginRequest
import com.example.travelapp.data.model.Setting.NotificationRequest
import com.example.travelapp.data.model.SocialLoginRequest
import com.example.travelapp.data.model.SocialLoginResponse
import com.example.travelapp.data.model.TokenResponse
import com.example.travelapp.data.model.UpdateProfileRequest
import com.example.travelapp.data.model.User
import com.example.travelapp.data.model.WithdrawResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {
    // 소셜 로그인(토큰 교환 방식)
    // 반환 타입을 SocialLoginResponse로 변경 (TokenResponse 없음)
    @POST("api/auth/social-login")
    suspend fun socialLogin(@Body request: SocialLoginRequest): Response<SocialLoginResponse>

    // 일반 로그인도 SocialLoginResponse 구조를 공유한다고 가정 (서버 응답 형태 확인 필요)
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<SocialLoginResponse>

    @GET("api/auth/mypage")
    suspend fun getMyProfile(): Response<User>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    @PUT("api/auth/notification")
    suspend fun updateNotificationSetting(
        @Body response: NotificationRequest
    ): Response<Unit>

    @POST("api/auth/withdraw")
    suspend fun withdraw(): Response<WithdrawResponse>

    // 토큰 갱신 API 추가
    // Authenticator에서 .execute()로 동기 호출하기 위해 Call 사용.
    @POST("api/auth/refresh")
    fun refreshTokens(
        @Header("Authorization") refreshToken: String
    ): retrofit2.Call<TokenResponse>

    @POST("api/auth/updateProfile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<Unit>>
}