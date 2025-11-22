package com.example.travelapp.data.repository

import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.CreatePostRequest
import com.example.travelapp.data.model.Post
import javax.inject.Inject

// Hilt가 이 Repo를 생성할 때 필요한 의존성을 자동으로 주입할 수 있도록 @Inject 사용
// PostApiService는 NetworkModule에서 @Provides로 제공되므로 Hilt가 이를 주입해 줄 수 있다.
class PostRepository @Inject constructor(
    private val postApiService: PostApiService // Retrofit으로 생성된 PostApiServices 인스턴스 주입
) {
    // 게시물 생성하는 함수
    // suspend 키워드 사용해 코루틴 내 비동기적으로 실행될 수 있도록 함.
    // Result<Post> 반환해 성공 or 실패 상태와 함께 데이터 안전하게 전달함.
    suspend fun createPost(request: CreatePostRequest): Result<Post> {
        return try {
            // PostApiService 통해 실제 네트워크 요청 수행
            val response = postApiService.createPost(request)
            if(response.isSuccessful) {
                // 응답이 성공적이면 본문(body)를 Result.success 래핑해 반환
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(IllegalStateException("API 응답 본문이 비어있습니다."))
            } else {
                // 응답이 성공적이지 않으면 HTTP 에러를 Result.failure 반환함
                Result.failure(RuntimeException("게시물 생성 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            // 네트워크 오류 등 예외 발생 시 Result.failure 반환함.
            Result.failure(e)
        }
    }
}