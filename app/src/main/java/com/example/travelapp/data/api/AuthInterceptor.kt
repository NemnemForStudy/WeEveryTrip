package com.example.travelapp.data.api

import com.example.travelapp.util.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 모든 API 요청 헤더에 JWT Access Token 자동으로 추가하는 인터셉터
 */

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // 저장된 토큰 가져오기
        val token = tokenManager.getToken()

        // 토큰이 있으면 헤더에 추가
        if(!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}