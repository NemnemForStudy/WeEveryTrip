package com.nemnem.travelapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoJsonPoint(
    val type: String, // 기본값(= "Point") 제거 (무조건 입력받게 변경)
    // GeoJSON 표준 : [경도, 위도] 순서
    val coordinates: List<Double>
)