package com.example.travelapp.data.api

import com.example.travelapp.data.model.SocialLoginRequest
import com.example.travelapp.data.model.SocialLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/social-login")
    // @Body는 파라미터(request)를 HTTP 요청의 본문에 담아 보내도록 함.
    // 포함하는 응답을 비동기적(suspend) 받음.
    suspend fun socialLogin(@Body request: SocialLoginRequest): Response<SocialLoginResponse>
}