package com.nemnem.travelapp.data.api

import com.nemnem.travelapp.util.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. 저장된 Access Token을 가져옴
        val token = tokenManager.getAccessToken() ?: tokenManager.getToken()

        // 2. 토큰이 있다면 모든 요청 헤더에 삽입
        val request = originalRequest.newBuilder().apply {
            token?.let {
                addHeader("Authorization", "Bearer $it")
            }
        }.build()

        return chain.proceed(request)
    }
}