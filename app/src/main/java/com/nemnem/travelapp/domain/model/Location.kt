package com.nemnem.travelapp.domain.model

import com.google.gson.annotations.SerializedName
import kotlin.math.*

data class Location(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("location_name")
    val name: String,
    @SerializedName("location_id")
    val id: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("is_Domestic")
    val isDomestic: Boolean
) {
    private val EARTH_RADIUS_KM = 6371.0
//    // ⭐️ TDD Red Phase: 테스트를 컴파일시키기 위한 최소 구현
//    // 실제 거리 계산 로직은 다음 단계(Green Phase)에서 Haversine 공식을 사용하여 구현
//    // 현재는 0.0을 반환하여 distanceTo 테스트를 실패시킴
//    fun distanceTo(other: Location): Double {
//        return 0.0
//    }

    fun distanceTo(other: Location): Double {
        val latRad1 = Math.toRadians(this.latitude)
        val latRad2 = Math.toRadians(other.latitude)

        val dLat = Math.toRadians(other.latitude - this.latitude)
        val dLon = Math.toRadians(other.longitude - this.longitude)

        val a = sin(dLat / 2).pow(2) +
                sin(dLon / 2).pow(2) * cos(latRad1) * cos(latRad2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * TDD Green Phase: 좌표 유효성 검사 구현
     * 위도: -90 ~ 90, 경도: -180 ~ 180 범위 내에 있어야 합니다.
     */
    fun isValid(): Boolean {
        return (latitude in -90.0..90.0) && (longitude in -180.0..180.0)
    }
}