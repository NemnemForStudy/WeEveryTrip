package com.example.travelapp.data.api

import com.example.travelapp.data.model.CreatePostRequest
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.RouteRequest
import com.example.travelapp.data.model.RouteResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PostApiService {
    // 게시물 생성 api 엔드포인트
    // suspend 키워드는 이 함수가 코루틴 내 비동기적으로 실행될 수 있음을 나타냄.
    // @Body 어노테이션은 createPost 함수의 'request' 파라미터가 HTTP 요청의 본문(JSON)으로 전송
    // Response<Post>는 Retrofit이 서버 응답을 Post 데이터 클래스로 변환해 반환,
    // HTTP 응답 상태 코드 등 더 많은 정보를 포함하는 Response 객체로 래핑함.

    @GET("api/posts") // 모든 게시물 가져오기
    suspend fun getAllPosts(): Response<List<Post>>

    @GET("api/posts") // 제목으로 검색
    suspend fun searchPosts(@Query("search") query: String): Response<List<Post>>

    @Multipart
    @POST("api/posts")
    suspend fun createPost(
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("category") category: RequestBody,
        @Part("coordinates") coordinates: RequestBody?,
        @Part("isDomestic") isDomestic: RequestBody,
        @Part("tags") tags: RequestBody?,
        @Part images: List<MultipartBody.Part>
    ): Response<Post> // ApiResponse 제거하고 Post로 직접 반환

    @POST("api/routes/route-for-day")
    suspend fun getRouteForDay(@Body request: RouteRequest): Response<RouteResponse>

    @GET("api/posts/{id}")
    suspend fun getPostById(
        @Path("id") postId: String
    ): Post // 배열이 아니라 단일 객체 반환
}