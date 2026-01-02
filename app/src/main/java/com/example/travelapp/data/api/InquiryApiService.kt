package com.example.travelapp.data.api

import com.example.travelapp.data.model.email.InquiryRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface InquiryApiService {
    @POST("api/send/email")
    // suspend - 비동기 통신 위해 코루틴 사용.
    // @Body - 객체를 JSON 형태로 변환해 HTTP 본문에 담아 보내겠다는 의미.
    // Unit - 반환값 없을 때 Unit 쓰는 것이 효율적임.
    suspend fun sendEmail(@Body request: InquiryRequest) : Response<Unit>
}