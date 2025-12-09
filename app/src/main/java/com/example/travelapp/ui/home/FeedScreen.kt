package com.example.travelapp.ui.home

import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.model.Post
import com.example.travelapp.ui.components.BottomNavigationBar
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.Beige
import com.example.travelapp.util.UtilTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    navController: NavHostController,
    viewModel: FeedViewModel = hiltViewModel(),
    // 만약 이 매개변수에 아무것도 넘겨주지 않으면, {} (비어있는 람다 식)이 기본으로 사용
    onPostClick: (Post) -> Unit = {}
) {
    val post by viewModel.post.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    Scaffold (
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = Screen.Feed.route
            )
        }
    ){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Beige)
                .padding(paddingValues)
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
                            color = Color.Gray,
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
 * 기능:
 * - 검색어 입력
 * - 검색 아이콘 클릭
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
            placeholder = { Text("게시물 검색...", fontSize = 14.sp, color = Color.Gray) },
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
                    contentColor = if (isSelected) Color.White else Color.Gray
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
@RequiresApi(Build.VERSION_CODES.O)
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 썸네일 이미지 또는 기본 배경
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                val thumbnailUrl = post.imgUrl

                // 이미지가 있을때는 이미지 보여주기
                if (thumbnailUrl != null) {
                    val imageRequest = remember(thumbnailUrl) {
                        ImageRequest.Builder(context)
                            .data("${BuildConfig.BASE_URL}${thumbnailUrl}")
                            .crossfade(true)
                            .size(300)
                            .memoryCacheKey(thumbnailUrl)
                            .diskCacheKey(thumbnailUrl)
                            .allowHardware(false)
                            .build()
                    }

                    val painter  = rememberAsyncImagePainter(model = imageRequest)

                    when(painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            // 로딩 중 UI
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is AsyncImagePainter.State.Success -> {
                            // 이미지 로드 성공
                            Image(
                                painter = painter,
                                contentDescription = "썸네일",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            // 에러 또는 기타 상태
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "이미지 로드 실패",
                                tint = Color.Gray
                            )
                        }
                    }
                } else {
                    // 이미지 없는 경우
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "이미지 없음",
                        tint = Color.Gray
                    )
                }
            }
            // 게시물 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = post.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // 작성자 및 날짜
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.nickname,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = UtilTime.formatRelativeTime(post.created_at),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 태그
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val safeTags = post.tags ?: emptyList()
                    // null 이면 빈 리스트
                    safeTags.take(2).forEach { tag ->
                        Text(
                            text = "#$tag",
                            fontSize = 11.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                    if (safeTags.size > 2) {
                        Text(
                            text = "+${safeTags.size - 2}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
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
            images = emptyList(),
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
            images = emptyList(),
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
            images = emptyList(),
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
