package com.example.travelapp.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.*

@OptIn(ExperimentalNaverMapApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    initialLat: Double? = null,
    initialLon: Double? = null
) {
    // 초기 카메라 위치 설정(값 넘어오면 그곳, 아니면 서울 시청)
    val cameraPositionState = rememberCameraPositionState {
        val target = if(initialLat != null && initialLon != null) {
            LatLng(initialLat, initialLon)
        } else {
            LatLng(37.5665, 126.9779) // 서울 시청
        }
        position = CameraPosition(target, 15.0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("지도 보기") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        NaverMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState,
        ) {
            // 넘어온 위치에 마커 표시
            if(initialLat != null && initialLon != null) {
                Marker(
                    state = MarkerState(position = LatLng(initialLat, initialLon)),
                    captionText = "선택된 위치"
                )
            }
        }
    }
}