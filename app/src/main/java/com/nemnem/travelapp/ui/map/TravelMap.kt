package com.nemnem.travelapp.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nemnem.travelapp.data.model.Post
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.util.MarkerIcons

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun TravelMap(
    posts: List<Post>,
    modifier: Modifier = Modifier,
    initialLat: Double? = null,
    initialLon: Double? = null
) {
    // 카메라 위치 상태 관리 (초기 위치는 서울 시청 정도로 설정하거나, 첫 번째 포스트 위치로 설정 가능)
    val initialPosition = remember(posts, initialLat, initialLon) {
        // 외부에서 넘겨준 초기 좌표가 유효한지 확인
        if(initialLat != null && initialLon != null && initialLat != 0.0 && initialLon != 0.0) {
            LatLng(initialLat, initialLon)
        }
        // 게시물 리스트가 있다면 첫 번째 게시물의 위치 사용
        else {
            posts.firstOrNull() { it.latitude != null && it.longitude != null }?.let { post ->
                LatLng(post.latitude!!, post.longitude!!)
            } ?: LatLng(37.5665, 126.9779)
        }
    }

    val cameraPositionState = rememberCameraPositionState{
        position = com.naver.maps.map.CameraPosition(initialPosition, 10.0)
    }

    NaverMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            isZoomControlEnabled = true,
            isLocationButtonEnabled = false
        )
    ) {
        // Post 리스트를 순회하며 마커를 표시합니다.
        posts.forEach { post ->
            // 위도와 경도가 null이 아닌 경우에만 마커 생성
            val lat = post.latitude
            val lng = post.longitude
            if (lat != null && lng != null) {
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    captionText = post.title, // 마커 아래에 제목 표시
                    icon = MarkerIcons.BLUE, // 마커 색상 (기본 파란색)
                    captionMinZoom = 12.0, // 줌이 멀어지면 글씨 숨김 (깔끔하게)
                    onClick = {
                        // TODO: 마커 클릭 시 이벤트 (나중에 Feed 연동 시 구현)
                        false // true 반환 시 지도 클릭 이벤트 무시, false면 지도 클릭 이벤트 전파
                    }
                )
            }
        }
    }
}