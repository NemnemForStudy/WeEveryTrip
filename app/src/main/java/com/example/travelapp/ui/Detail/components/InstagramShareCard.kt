package com.example.travelapp.ui.Detail.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.data.model.Post
import com.example.travelapp.util.MapUtil
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import org.jetbrains.annotations.Async

// 피드 캐러셀용 '요약 카드'(잡지 스타일)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InstagramShareCard(
    post: Post,
    mapBitmap: Bitmap,
    onCaptured: (Bitmap) -> Unit
) {
    val controller = rememberCaptureController()

    Column(
        modifier = Modifier
            .capturable(controller)
            .width(350.dp)
            .background(Color.White)
            .padding(24.dp)
    ) {
        AsyncImage(
            model = MapUtil.toFullUrl(post.images?.firstOrNull()),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(20.dp))

        Text(post.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
        Text("Travel with ${post.nickname}", fontSize = 14.sp, color = Color.Gray)

        // 지도 스냅샷
        if(mapBitmap != null) {
            Image(
                bitmap = mapBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(24.dp))

            // 하단 브랜딩
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF4A90E2), modifier = Modifier.size(20.dp))
                Text(" ModuTrip", fontWeight = FontWeight.Black, color = Color(0xFF4A90E2), fontSize = 16.sp)
            }
        }

        // 그려지지마자 캡처 트리거
        LaunchedEffect(Unit) {
            controller.capture()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InstagramStorySticker(
    post: Post,
    onCaptured: (Bitmap) -> Unit
) {
    val controller = rememberCaptureController()

    // 스티커는 배경이 투명하거나 그림자가 있는 작은 박스 형태
    Box(modifier = Modifier.padding(20.dp).capturable(controller)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 12.dp,
            modifier = Modifier.width(260.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 작은 썸네일
                AsyncImage(
                    model = MapUtil.toFullUrl(post.images?.firstOrNull()),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        post.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text("나의 여행 경로 보기", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        controller.capture()
    }
}