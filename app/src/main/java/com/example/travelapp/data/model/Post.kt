package com.example.travelapp.data.model

import androidx.compose.ui.layout.LayoutCoordinates
import com.google.gson.annotations.SerializedName
import com.kakao.sdk.template.model.Content
import java.io.Serial
import java.io.Serializable
import java.sql.Timestamp

// 좌표 정보 담을 데이터 클래스
data class Post(
    // 어노테이션을 추가하여 서버의 'post_id'와 매핑합니다.
    @SerializedName("post_id") val id: String, // 서벙서 할당되는 ID
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("comment_count") val commentCount: Int = 0,
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
    @SerializedName(value = "coordinates", alternate = ["coordinate"])
    val coordinate: GeoJsonPoint? = null,

    @SerializedName("location_name")
    val locationName: String? = null, // 위치 이름

    @SerializedName("is_domestic")
    val isDomestic: Boolean = true, // 국내(true) / 국외(false) 여행 구분

    @SerializedName("travel_start_date")
    val travelStartDate: String? = null,

    @SerializedName("travel_end_date")
    val travelEndDate: String? = null,

    @SerializedName("image_locations")
    val imageLocations: List<PostImageLocation> = emptyList()
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

data class UpdateImageLocationRequest(
    val imageUrl: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dayNumber: Int? = null,
    val sortIndex: Int? = null,
    val timestamp: Long? = null
)

data class UpdatePostRequest(
    val category: String? = null,
    val title: String? = null,
    val content: String? = null,
    val tags: List<String>? = null,
    val coordinate: GeoJsonPoint? = null,
    val locationName: String? = null,
    val isDomestic: Boolean? = null,
    val travelStartDate: String? = null,
    val travelEndDate: String? = null,
    val images: List<String>? = null,
    val imageLocations: List<UpdateImageLocationRequest>? = null
)

data class UpdatePostResponse(
    @SerializedName("post_id") val id: String,
    val category: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    @SerializedName("thumbnail_url")
    val imgUrl: String? = null,
    @SerializedName("coordinates")
    val coordinate: GeoJsonPoint? = null,
    @SerializedName("location_name")
    val locationName: String? = null,
    @SerializedName("is_domestic")
    val isDomestic: Boolean,
    @SerializedName("updated_at")
    val updateAt: String
)

// ✅ 서버에서 post_image 테이블에서 뽑아 내려줄 “사진별 위치 데이터”
// - image_url: 어떤 이미지의 좌표인지 식별용
// - latitude/longitude: 마커 찍을 때 쓰는 좌표
// - day_number/sort_index: 나중에 Day별 polyline 그릴 때 정렬 기준
data class PostImageLocation(
    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("day_number")
    val dayNumber: Int? = null,

    @SerializedName("sort_index")
    val sortIndex: Int? = null,

    @SerializedName("timestamp")
    val timestamp: Long ?= null
)

data class DeletePostRequest(
    @SerializedName("post_id") val postId: Long
)

data class UploadImagesResponse(
    val success: Boolean,
    val message: String? = null,
    val urls: List<String> = emptyList()
)