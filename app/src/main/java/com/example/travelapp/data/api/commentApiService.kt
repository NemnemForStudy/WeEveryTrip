package com.example.travelapp.data.api

import com.example.travelapp.data.model.ApiResponse
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.data.model.comment.CreateCommentRequest
import com.example.travelapp.data.model.comment.UpdateCommentRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CommentApiService {
    @POST("api/posts/{postId}/comment")
    suspend fun createComment(
        @Path("postId") postId: String,
        @Body request: CreateCommentRequest
    ): Response<ApiResponse<Comment>>

    @GET("api/posts/{postId}/comments")
    suspend fun getComments(
        @Path("postId") postId: String,
    ): Response<ApiResponse<List<Comment>>>

    @PUT("api/posts/comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: String,
        @Body request: UpdateCommentRequest
    ): Response<ApiResponse<Comment>>

    @DELETE("api/posts/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: String
    ): Response<ApiResponse<Unit>>
}