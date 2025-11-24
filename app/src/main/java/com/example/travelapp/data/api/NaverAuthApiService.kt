package com.example.travelapp.data.api

import com.example.travelapp.data.model.NaverProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
interface NaverAuthApiService {
    @GET("v1/nid/me")
    suspend fun getNaverUserProfile(
        @Header("Authorization") authorization: String
    ): Response<NaverProfileResponse>
}