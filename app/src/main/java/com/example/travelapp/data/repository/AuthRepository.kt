package com.example.travelapp.data.repository

import com.example.travelapp.data.api.AuthApiService
import com.example.travelapp.data.model.SocialLoginRequest
import com.example.travelapp.data.model.SocialLoginResponse
import retrofit2.Response
import javax.inject.Inject

// 인증 관련 처리 담당 클래스
// Hilt 통해 AuthApiService 구현체 주입 받음
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService
) {
    // ViewModel에서 함수 호출하게 됨.
    // 파라미터로 받은 SocialLoginRequest 객체를 ApiService에 그대로 전달
    // Request(요청서)를 서버로 보내고 Response에 담에서 그대로 되돌려줌.
    suspend fun socialLogin(request: SocialLoginRequest): Response<SocialLoginResponse> {
        return apiService.socialLogin(request)
    }
}