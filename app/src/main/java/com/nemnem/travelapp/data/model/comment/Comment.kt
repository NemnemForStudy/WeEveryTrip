package com.nemnem.travelapp.data.model.comment

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("comment_id")
    val commentId: String,
    @SerializedName("post_id")
    val postId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("nickname")
    val nickname: String? = null,
    @SerializedName("profile_image")
    val profileImage: String? = null
)