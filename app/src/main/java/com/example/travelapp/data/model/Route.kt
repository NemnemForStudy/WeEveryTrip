package com.example.travelapp.data.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)

// 서버로 보낼 때: { "locations": [...]
data class RouteRequest(
    val locations: List<RoutePoint>
)

// 서버에서 받을 때
data class RouteResponse(
    val route: List<RoutePoint>
)