package com.example.travelapp.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id")
    val id: Int,
    val email: String,
    val nickname: String,
    @SerializedName("profile_image")
    val profileImage: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("post_count")
    val postCount: Int = 0,
    @SerializedName("like_count")
    val likeCount: Int = 0,
    @SerializedName("comment_count")
    val commentCount: Int = 0
)