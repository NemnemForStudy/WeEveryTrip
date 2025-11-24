package com.example.travelapp.util

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * API 요청을 가로채 'Authorization' 헤더에 JWT 토큰을 추가하는 인터셉터
 * @param tokenManager SharedPreferences 토큰을 관리하는 매니저
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // 토큰 가져옴
        val token = tokenManager.getToken()

        // 원래 요청을 복사해 새 요청 만듦
        val request = chain.request().newBuilder()

        // 토큰이 존재할 경우. "Authorization" 헤더에 "Bearer [토큰값]" 형태로 추가함.
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }

        // 수정된 헤더 포함한 요청 계속 진행
        return chain.proceed(request.build())
    }
}