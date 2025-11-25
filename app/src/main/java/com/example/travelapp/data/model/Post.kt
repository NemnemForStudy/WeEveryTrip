package com.example.travelapp.data.model

import com.google.gson.annotations.SerializedName

data class Post(
    // 어노테이션을 추가하여 서버의 'post_id'와 매핑합니다.
    @SerializedName("post_id") val id: String, // 서벙서 할당되는 ID
    val category: String,
    val title: String,
    val content: String,
    val nickname: String,
    val created_at: String,
    val tags: List<String>,
    val imgUrl: String? = null // 이미지 있을 경우 URL
)

// 게시물 생성 요청위한 데이터 클래스(ID는 서버에서 생성하므로 제외)
data class CreatePostRequest(
    val category: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val imgUrl: String? = null
)