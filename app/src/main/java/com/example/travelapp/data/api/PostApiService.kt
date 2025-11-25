package com.example.travelapp.data.api

import com.example.travelapp.data.model.CreatePostRequest
import com.example.travelapp.data.model.Post
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

interface PostApiService {
    // 게시물 생성 api 엔드포인트
    // suspend 키워드는 이 함수가 코루틴 내 비동기적으로 실행될 수 있음을 나타냄.
    // @Body 어노테이션은 createPost 함수의 'request' 파라미터가 HTTP 요청의 본문(JSON)으로 전송
    // Response<Post>는 Retrofit이 서버 응답을 Post 데이터 클래스로 변환해 반환,
    // HTTP 응답 상태 코드 등 더 많은 정보를 포함하는 Response 객체로 래핑함.

    @GET("posts") // 모든 게시물 가져오기
    suspend fun getPosts(): Response<List<Post>>

    @GET("api/posts") // 제목으로 검색
    suspend fun searchPosts(@Query("search") query: String): Response<List<Post>>

    @Multipart
    @POST("/api/posts") // TODO: 실제 백엔드 API 엔드포인트 경로로 변경해야 함.
    suspend fun createPost(
        // 텍스트 데이터들을 Map 형태로 받음
        @PartMap textData: Map<String, @JvmSuppressWildcards RequestBody>,
        // 이미지 파일들을 리스트 형태로 받음
        @Part images: List<MultipartBody.Part>
//        @Body request: CreatePostRequest
    ): Response<Post>
}