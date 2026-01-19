package com.nemnem.travelapp.util


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naver.maps.geometry.LatLng
import androidx.compose.ui.graphics.Color
import com.naver.maps.map.compose.LineCap
import com.naver.maps.map.compose.LineJoin
import com.naver.maps.map.compose.PolylineOverlay
//import com.naver.maps.map.compose.PolylineOverlay
import kotlinx.coroutines.delay

@Composable
fun AnimatedPolyline(
    coords: List<LatLng>,
    color: Color = Color(0xFF1E88E5),
    width: Dp = 6.dp,
    zIndex: Int = 1,
    stepDelayMs: Long = 6L, // 선이 늘어나는 속도
    pauseIndices: Set<Int> = emptySet(),
    pauseDelayMs: Long = 300L // 사진 지점에서 멈추는 시간
) {
    val animatedCoords = remember(coords) { mutableStateListOf<LatLng>() }

    LaunchedEffect(coords) {
        animatedCoords.clear()
        if (coords.isEmpty()) return@LaunchedEffect
        animatedCoords.add(coords[0]) // 시작지점

        for (i in 0 until coords.size - 1) {
            val start = coords[i]
            val end = coords[i + 1]

            // 구간 10개로 쪼개서 부드럽게 연결
            val segments = 3
            for(step in 1..segments) {
                val progress = step.toFloat() / segments
                val lat = start.latitude + (end.latitude - start.latitude) * progress
                val lng = start.longitude + (end.longitude - start.longitude) * progress
                val currentPoint = LatLng(lat, lng)

                if(animatedCoords.size > i + 1) {
                    animatedCoords[animatedCoords.size - 1] = currentPoint
                } else {
                    animatedCoords.add(currentPoint)
                }
                delay(stepDelayMs)
            }

            // 사진이 있는 지점이거나 도착지면 잠시 멈춤
            if(i + 1 in pauseIndices || i + 1 == coords.size - 1) {
                delay(pauseDelayMs)
            }
        }
    }

    if (animatedCoords.size >= 2) {
        PolylineOverlay(
            coords = animatedCoords.toList(),
            color = color,
            width = width,
            zIndex = zIndex,
            capType = LineCap.Round,
            joinType = LineJoin.Round
        )
    }
}
