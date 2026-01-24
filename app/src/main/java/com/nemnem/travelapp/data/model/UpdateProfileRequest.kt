package com.nemnem.travelapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * 프로필 수정을 위해 서버에 전달하는 데이터 객체입니다.
 */
@Keep
@Serializable
data class UpdateProfileRequest(
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("profileImageUrl")
    val profileImageUrl: String?
)