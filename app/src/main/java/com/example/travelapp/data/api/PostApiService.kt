package com.example.travelapp.data.api

import com.example.travelapp.data.model.CreatePostRequest
import com.example.travelapp.data.model.Post
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PostApiService {
    // 게시물 생성 api 엔드포인트
    // suspend 키워드는 이 함수가 코루틴 내 비동기적으로 실행될 수 있음을 나타냄.
    // @Body 어노테이션은 createPost 함수의 'request' 파라미터가 HTTP 요청의 본문(JSON)으로 전송
    // Response<Post>는 Retrofit이 서버 응답을 Post 데이터 클래스로 변환해 반환,
    // HTTP 응답 상태 코드 등 더 많은 정보를 포함하는 Response 객체로 래핑함.
    @POST("/api/posts") // TODO: 실제 백엔드 API 엔드포인트 경로로 변경해야 함.
    suspend fun createPost(@Body request: CreatePostRequest): Response<Post>
}