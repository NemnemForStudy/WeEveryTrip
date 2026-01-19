package com.nemnem.travelapp.data.model

import kotlinx.serialization.Serializable

/**
 * 프로필 수정을 위해 서버에 전달하는 데이터 객체입니다.
 */
@Serializable
data class UpdateProfileRequest(
    val nickname: String,
    val profileImageUrl: String?
)