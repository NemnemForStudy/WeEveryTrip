package com.example.travelapp.data.model

import androidx.compose.ui.layout.LayoutCoordinates
import com.google.gson.annotations.SerializedName
import java.io.Serializable

// 좌표 정보 담을 데이터 클래스
data class Post(
    // 어노테이션을 추가하여 서버의 'post_id'와 매핑합니다.
    @SerializedName("post_id") val id: String, // 서벙서 할당되는 ID
    val category: String,
    val title: String,
    val content: String,
    val nickname: String,
    val created_at: String,
    val tags: List<String> = emptyList(),
    val images: List<String> = emptyList(),

    @SerializedName("thumbnail_url")
    val imgUrl: String? = null, // 이미지 있을 경우 URL

    // 위도/경도 개별 필드 삭제 -> GeoJsonPoint로 통합
    @SerializedName("coordinates")
    val coordinate: GeoJsonPoint? = null,

    @SerializedName("location_name")
    val locationName: String? = null, // 위치 이름

    @SerializedName("is_domestic")
    val isDomestic: Boolean = true // 국내(true) / 국외(false) 여행 구분
) {
    // Getter 추가
    val latitude: Double?
        get() = coordinate?.coordinates?.getOrNull(1) // 위도 (Latitude)

    val longitude: Double?
        get() = coordinate?.coordinates?.getOrNull(0) // 경도 (Longitude)
}

// 게시물 생성 요청위한 데이터 클래스(ID는 서버에서 생성하므로 제외)
data class CreatePostRequest(
    val category: String,
    val title: String,
    val content: String,
    val tags: List<String>,
//    val imgUrl: String? = null,
    val coordinate: GeoJsonPoint? = null,
    val locationName: String? = null,
    val isDouble: Boolean = true
)

// 게시물 생성 응답용 데이터 클래스
data class CreatePostResponse(
    @SerializedName("post_id") val id: String,
    val title: String,
    val created_at: String,
    val images: List<String>
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?
)