package com.example.travelapp.ui.home

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.model.Post
import com.example.travelapp.ui.theme.Beige
import android.text.format.DateUtils // DateUtils에 필요
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.navigation.NavHostController
import com.example.travelapp.ui.components.BottomNavigationBar
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.TextSub
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Instant

/**
 * 게시판(피드) 화면 Composable
 *
 * 구성 요소:
 * - 검색 바
 * - 카테고리 탭
 * - 게시물 목록 (LazyColumn으로 무한 스크롤 구현)
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel(),
    onPostClick: (Post) -> Unit = {}
) {
    val post by viewModel.post.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    // 🔥 1. Scaffold로 감싸기
    Scaffold(
        bottomBar = {
            // 🔥 2. 하단 바 추가
            // navController가 NavHostController 타입일 때만 표시 (프리뷰 등에서의 에러 방지)
            if (navController is NavHostController) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = Screen.Feed.route
                )
            }
        }
    ) { paddingValues -> // 🔥 3. Scaffold가 주는 여백값 받기

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Beige)
                .padding(paddingValues) // 🔥 4. 여기서 패딩을 적용해야 하단 바에 내용이 안 가려짐!
        ) {
            // 상단 검색 바
            CustomSearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            // 카테고리 탭
            CategoryTabs(
                categories = viewModel.categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 게시물 목록
            when {
                isLoading && post.isEmpty() -> {
                    // 로딩 상태
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMsg != null -> {
                    // 에러 상태
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMsg ?: "오류가 발생했습니다.",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
                post.isEmpty() -> {
                    // 빈 상태
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "게시물이 없습니다.",
                            color = Color(0xFF616161),
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    // 게시물 목록
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(post.size) { index ->
                            val currentPost = post[index]
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                PostCard(
                                    post = currentPost,
                                    onClick = {
                                        navController.navigate("detail/${currentPost.id}")
                                    }
                                )
                            }
                        }

                        // 무한 스크롤 트리거
                        if (post.isNotEmpty()) {
                            item {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMorePosts()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 검색 바 Composable
 *
 * 기능:
 * - 검색어 입력
 * - 검색 아이콘 클릭
 *
 * Material3의 SearchBar와 충돌을 피하기 위해 CustomSearchBar로 명명
 */
@Composable
fun CustomSearchBar(modifier: Modifier = Modifier) {
    // 이 변수의 값이 바뀌면 화면(UI)도 알아서 다시 그려라!
    var searchText by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("게시물 검색...", fontSize = 14.sp, color = Color(0xFF616161)) },
            modifier = Modifier
                .weight(1f)
                .background(Color.White),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        IconButton(onClick = { /* 검색 로직 */ }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "검색",
                tint = Color(0xFF1976D2)
            )
        }
    }
}

/**
 * 카테고리 탭 Composable
 *
 * 기능:
 * - 카테고리 목록 표시
 * - 선택된 카테고리 강조 표시
 * - 카테고리 선택 시 필터링
 */
@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            val isSelected = category == selectedCategory

            Button(
                onClick = { onCategorySelected(category) },
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF1976D2) else Color.White,
                    contentColor = if (isSelected) Color.White else Color(0xFF616161)
                ),
                shape = RoundedCornerShape(20.dp),
                border = if (!isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                } else null
            ) {
                Text(
                    text = category,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 게시물 카드 Composable
 *
 * 구성:
 * - 썸네일 이미지 (또는 기본 배경)
 * - 제목
 * - 작성자 및 작성 날짜
 * - 태그 목록
 */
@Composable
fun PostCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 왼쪽 썸네일 이미지 영역
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (!post.imgUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(toFullUrl(post.imgUrl)) // 📍 아래 정의된 함수 사용
                            .crossfade(true)
                            .build(),
                        contentDescription = "썸네일",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.LightGray
                    )
                }
            }

            // 2. 오른쪽 게시물 정보 영역
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 제목
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1, // 한 줄로 깔끔하게 처리
                    modifier = Modifier.fillMaxWidth()
                )

                // 닉네임 및 시간
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.nickname,
                        fontSize = 12.sp,
                        color = Color(0xFF444444),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = " • ", fontSize = 12.sp, color = TextSub)
                    Text(
                        text = formatRelativeTime(post.created_at), // 📍 아래 정의된 함수 사용
                        fontSize = 12.sp,
                        color = TextSub
                    )
                }

                // 태그 및 하단 아이콘 (좋아요, 댓글)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 태그 (최대 2개)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        post.tags?.take(2)?.forEach { tag ->
                            Text(text = "#$tag", fontSize = 11.sp, color = Color(0xFF1976D2))
                        }
                    }

                    // 반응 (좋아요, 댓글)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReactionItem(Icons.Default.FavoriteBorder, post.likeCount.toString())
                        ReactionItem(Icons.Outlined.ChatBubbleOutline, post.commentCount.toString())
                    }
                }
            }
        }
    }
}

/**
 * [DRY 원칙] 좋아요/댓글 아이콘 세트 함수화
 */
@Composable
fun ReactionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Text(text = count, fontSize = 11.sp, color = Color.Gray)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatIsoDateTime(isoString: String): String {
    return try {
        // 1. ZonedDateTime 객체로 파싱 (끝의 Z는 UTC 기준을 의미하므로 ZonedDateTime 사용)
        val zonedDateTime = ZonedDateTime.parse(isoString)

        // 원하는 출력 형식 정의
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.getDefault())
        zonedDateTime.toLocalDateTime().format(formatter)
    } catch (e: Exception) {
        "날짜 형식 오류"
    }
}

/**
 * ISO 8601 형식 날짜를 현재 시간과의 상대적인 시간으로 포매팅
 */
fun formatRelativeTime(timeString: String?): String {
    // 1. null 체크 (String?로 받으면 null이 들어올 수 있어서 안전하게 체크!)
    if (timeString.isNullOrEmpty()) return "방금 전"

    return try {
        // 2. 글자 자르기 (이건 API 레벨 상관없이 다 작동해요)
        // "2023-12-01T10:00:00" -> ["2023-12-01", "10:00:00"] 로 나눠서 앞부분만 가져옴
        timeString.split("T")[0]
    } catch (e: Exception) {
        // 3. 혹시라도 형식이 이상하면 앱이 꺼지지 않게 기본값 반환
        "방금 전"
    }
}

/**
 * Preview: 게시판 화면 미리보기
 *
 * 이 함수는 Android Studio의 Preview 기능으로
 * 실제 앱을 실행하지 않고도 UI를 확인할 수 있습니다.
 *
 * Preview에서는 hiltViewModel()을 사용할 수 없으므로,
 * 더미 데이터를 직접 전달하여 UI만 미리보기합니다.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, heightDp = 800)
fun FeedScreenPreview() {
    // Preview용 더미 게시물 데이터
    val dummyPosts = listOf(
        Post(
            id = "1",
            category = "여행 후기",
            title = "서울 3일 여행 코스 추천",
            content = "서울의 명소를 효율적으로 돌아보는 방법을 소개합니다.",
            nickname = "여행러",
            created_at = "2024-11-28",
            tags = listOf("서울", "3일", "추천"),
            imgUrl = null
        ),
        Post(
            id = "2",
            category = "여행 팁",
            title = "비행기 탈 때 짐 싸는 팁",
            content = "효율적인 짐 싸기 방법을 알려드립니다.",
            nickname = "팩킹마스터",
            created_at = "2024-11-27",
            tags = listOf("팁", "짐", "여행"),
            imgUrl = null
        ),
        Post(
            id = "3",
            category = "추천 장소",
            title = "제주도 숨은 카페 5곳",
            content = "관광객이 잘 모르는 제주도의 멋진 카페들을 소개합니다.",
            nickname = "카페러버",
            created_at = "2024-11-26",
            tags = listOf("제주도", "카페", "숨은명소"),
            imgUrl = null
        )
    )
    
    // Preview용 간단한 Column으로 UI 표시
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 검색 바
        CustomSearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // 카테고리 탭 (Preview용 간단한 버전)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("전체", "여행 후기", "여행 팁", "질문", "추천 장소").forEach { category ->
                Button(
                    onClick = { },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (category == "전체") Color(0xFF1976D2) else Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(text = category, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 게시물 목록
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(dummyPosts.size) { index ->
                PostCard(
                    post = dummyPosts[index],
                    onClick = { }
                )
            }
        }
    }
}

private fun resolveBaseUrlForDevice(): String {
    val isEmulator = (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))

    val phoneBaseUrl = runCatching {
        BuildConfig::class.java.getField("PHONE_BASE_URL").get(null) as String
    }.getOrNull()

    val raw = if(isEmulator) {
        BuildConfig.BASE_URL
    } else {
        phoneBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL
    }

    return raw.trimEnd('/') + "/"
}

private fun toFullUrl(urlOrPath: String?): String? {
    if(urlOrPath.isNullOrBlank()) return null
    if(urlOrPath.startsWith("http")) return urlOrPath
    return resolveBaseUrlForDevice() + urlOrPath.trimStart('/')
}