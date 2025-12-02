package com.example.travelapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoJsonPoint(
    val type: String = "Point",
    // GeoJSON 표준 : [경도, 위도] 순서
    val coordinates: List<Double>
)