package com.example.travelapp.data.model.comment

import com.google.gson.annotations.SerializedName

/**
 * 댓글 작성(POST) 요청 시 서버로 보낼 데이터
 * Node.js에서 req.body.content 로 받는 부분과 매핑됩니다.
 */
data class CreateCommentRequest(
    @SerializedName("content")
    val content: String
)

/**
 * 댓글 수정(PUT/PATCH) 요청 시 서버로 보낼 데이터
 * (작성용과 구조가 같더라도, 명확한 구분을 위해 따로 만드는 것이 좋습니다)
 */
data class UpdateCommentRequest(
    @SerializedName("content")
    val content: String,
)