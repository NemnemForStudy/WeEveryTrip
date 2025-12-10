package com.example.travelapp.ui.Detail // 패키지명 확인

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.model.Post
import com.example.travelapp.util.UtilTime
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState

// 색상 상수
val PrimaryBlue = Color(0xFF4A90E2)
val LightGrayBg = Color(0xFFF5F7FA)
val TextGray = Color(0xFF888888)
val TextDark = Color(0xFF222222)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val post by viewModel.post.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val isLiked by viewModel.isLiked.collectAsStateWithLifecycle()
    val likeCount by viewModel.likeCount.collectAsStateWithLifecycle()

    // 화면 진입 시 딱 한 번 실행 (데이터 요청)
    LaunchedEffect(postId) {
        viewModel.fetchPostDetail(postId)
        viewModel.loadLikeData(postId)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            errorMsg != null -> {
                Text(
                    text = errorMsg ?: "알 수 없는 오류",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            post != null -> {
                PostDetailContent(
                    post = post!!,
                    isLiked = isLiked,
                    likeCount = likeCount,
                    onLikeToggle = { viewModel.toggleLike(postId) },
                    onBackClick = {
                        navController.navigate("feed") {
                            popUpTo("feed") { inclusive = true }
                        }
                    }
                )
            }
            // post 가 null, 에러 없고, 로딩도 아닐 떄 대비한 else
            else -> {
                // 데이터 로드에 실패했지만 에러 메시지가 없는 경우
                Text(
                    text = "게시물을 찾을 수 없습니다.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalNaverMapApi::class)
@Composable
fun PostDetailContent(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    onLikeToggle: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            // Material3 최신 버전에서는 SmallTopAppBar 대신 TopAppBar 사용 권장
            TopAppBar(
                title = {},
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.8f),
                        shadowElevation = 4.dp
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* 공유 기능 */ }) {
                        Icon(Icons.Default.Share, contentDescription = "공유", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            PostDetailBottomBar(
                isLiked = isLiked,
                likeCount = likeCount,
                onLikeClick = onLikeToggle,
                commentCount = 5
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // 대형 이미지 헤더
            val imageList = (post.images ?: emptyList()).ifEmpty { listOfNotNull(post.imgUrl) }
            val pagerState = rememberPagerState(
                initialPage = 1000, // 중간에서 시작
                pageCount = { 2000 } // 큰 수
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.LightGray)
            ) {
                if (imageList.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val actualIndex = page % imageList.size
                        val fullUrl = BuildConfig.BASE_URL + imageList[actualIndex]
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(fullUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "게시물 이미지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // 페이지 인디케이터
                if (imageList.size > 1) {
                    val currentIndex = pagerState.currentPage % imageList.size
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(imageList.size) { index ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentIndex == index) Color.White
                                        else Color.White.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Text("이미지가 없는 게시물입니다.", color = Color.White)
                    }
                }
            }

            // 내용 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 겹치는 효과를 주고 싶다면 offset 사용 가능 (.offset(y = (-20).dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                // 카테고리 태그
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = post.category ?: "카테고리 없음",
                        color = PrimaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 제목
                Text(
                    text = post.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark, // 🔥 이제 에러 안 남
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 작성자 프로필 & 날짜
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFE0E0E0)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(post.nickname.take(1), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = post.nickname, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        // 날짜 포맷 함수가 있다면 여기서 적용 (예: formatRelativeTime(post.created_at))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Text(
                                text = UtilTime.formatRelativeTime(post.created_at),
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color(0xFFE0E0E0))

                // 본문
                Text(
                    text = post.content,
                    fontSize = 16.sp,
                    color = Color(0xFF444444),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(30.dp))

                // 지도 영역
                Text("위치 정보", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                if (post.latitude != null && post.longitude != null) {
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition(LatLng(post.latitude!!, post.longitude!!), 14.0)
                    }
                    NaverMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        cameraPositionState = cameraPositionState
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("위치 정보 없음", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 태그 목록
                if(post.tags != null && post.tags.isNotEmpty()) {
                    Row {
                        post.tags.forEach { tag ->
                            Text(
                                text = "#$tag ", // 간격 띄우기 추가
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

/**
 * 하단 고정 바
 */
@Composable
fun PostDetailBottomBar (
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    commentCount: Int
) {
    Surface(
        shadowElevation = 16.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if(isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = if(isLiked) Color.Red else Color.Gray
                    )
                }

                Text(text = "${likeCount}", fontSize = 14.sp)

                Spacer(modifier = Modifier.width(16.dp))

                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "댓글", tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$commentCount", fontSize = 14.sp)
            }

            Button(
                onClick = {/*액션*/},
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("여행 일정 담기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 프리뷰
@Preview
@Composable
fun PostDetailScreenPreview() {
    val dummyPost = Post(
        id = "1",
        category = "국내여행",
        title = "강릉 안목해변 카페거리 정복기",
        content = "내용 예시입니다.",
        nickname = "바다조아",
        created_at = "2025-12-06",
        tags = listOf("강릉", "카페"),
        images = emptyList(),
        imgUrl = null
    )
//    PostDetailContent(post = dummyPost)
}