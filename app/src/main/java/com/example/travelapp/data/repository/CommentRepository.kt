package com.example.travelapp.data.repository

import android.content.Context
import com.example.travelapp.data.api.CommentApiService
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.data.model.comment.CreateCommentRequest
import javax.inject.Inject

open class CommentRepository @Inject constructor(
    private val commentApiService: CommentApiService,
) {
    /**
     * 댓글 작성 함수
     * ViewModel에서는 이 함수만 호출하면 됩니다.
     */
    suspend fun createComment(postId: String, content: String): Result<Comment> {
        return try{
            // viewModel에서 받은 텍스트를 DTO에 포장
            val request = CreateCommentRequest(content)

            // api 호출
            val response = commentApiService.createComment(postId, request)

            // 응답 처리
            if(response.isSuccessful) {
                val body = response.body()
                if(body != null && body.success && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "알 수 없는 오류"))
                }
            } else {
                Result.failure(Exception("서버 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            // 네트워크 끊김, 타임아웃 등 예외 발생
            Result.failure(e)
        }
    }

    suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val response = commentApiService.getComments(postId)

            if(response.isSuccessful) {
                val body = response.body()
                if(body != null && body.success && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message))
                }
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
