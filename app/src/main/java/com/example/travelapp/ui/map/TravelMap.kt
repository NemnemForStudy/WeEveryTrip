package com.example.travelapp.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.travelapp.data.model.Post
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.util.MarkerIcons

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun TravelMap(
    posts: List<Post>,
    modifier: Modifier = Modifier
) {
    // 카메라 위치 상태 관리 (초기 위치는 서울 시청 정도로 설정하거나, 첫 번째 포스트 위치로 설정 가능)
    val cameraPositionState = rememberCameraPositionState()

    NaverMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Post 리스트를 순회하며 마커를 표시합니다.
        posts.forEach { post ->
            // 위도와 경도가 null이 아닌 경우에만 마커 생성
            if (post.latitude != null && post.longitude != null) {
                Marker(
                    state = MarkerState(position = LatLng(post.latitude, post.longitude)),
                    captionText = post.title, // 마커 아래에 제목 표시
                    icon = MarkerIcons.BLUE, // 마커 색상 (기본 파란색)
                    onClick = {
                        // TODO: 마커 클릭 시 이벤트 (나중에 Feed 연동 시 구현)
                        false // true 반환 시 지도 클릭 이벤트 무시, false면 지도 클릭 이벤트 전파
                    }
                )
            }
        }
    }
}