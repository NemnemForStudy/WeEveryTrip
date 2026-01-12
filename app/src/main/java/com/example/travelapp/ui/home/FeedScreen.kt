package com.example.travelapp.ui.home

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // ✅ items(post)를 쓰기 위한 필수 임포트
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.travelapp.data.model.Post
import com.example.travelapp.ui.components.BottomNavigationBar
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.Beige
import com.example.travelapp.ui.theme.TextSub
import com.example.travelapp.util.MapUtil.toFullUrl
import com.example.travelapp.util.UtilTime

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

    // 1. 현재 화면의 백스택 관찰
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // 2. 신호 받기 (observeAsState 대신 getStateFlow 사용으로 에러 방지)
    val shouldRefresh by navBackStackEntry?.savedStateHandle
        ?.getStateFlow("should_refresh", false)
        ?.collectAsState() ?: remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    // DisposableEffect 쓰는 이유. 리스너 등록, 해제처럼 정리가 꼭 필요한 작업을 할 때 사용.
    // onDispose - 필수라 에러를 잡아줌.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_RESUME) {
                val refreshNeeded = navBackStackEntry?.savedStateHandle?.get<Boolean>("should_refresh") ?: false
                if(refreshNeeded) {
                    viewModel.loadPosts(forceRefresh = true)
                    navBackStackEntry?.savedStateHandle?.remove<Boolean>("should_refresh")
                }
            }
        }
        // 옵저버 추가.
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        bottomBar = {
            if (navController is NavHostController) {
                BottomNavigationBar(navController = navController, currentRoute = Screen.Feed.route)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Beige)
                .padding(paddingValues)
        ) {
            CustomSearchBar(modifier = Modifier.fillMaxWidth().padding(16.dp))

            CategoryTabs(
                categories = viewModel.categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading && post.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMsg != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMsg ?: "오류 발생", color = Color.Red)
                    }
                }
                post.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "게시물이 없습니다.")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // ✅ index 대신 items(post)를 사용하여 에러 방지
                        items(
                            items = post,
                            key = { it.id + (it.imgUrl ?: "") }
                        ) { currentPost ->
                            PostCard(
                                post = currentPost,
                                onClick = { navController.navigate("detail/${currentPost.id}") }
                            )
                        }

                        if (post.isNotEmpty()) {
                            item {
                                LaunchedEffect(Unit) { viewModel.loadMorePosts() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSearchBar(modifier: Modifier = Modifier) {
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
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        IconButton(onClick = { }) {
            Icon(Icons.Default.Search, null, tint = Color(0xFF1976D2))
        }
    }
}

@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray) else null
            ) {
                Text(text = category, fontSize = 12.sp)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostCard(post: Post, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(110.dp).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(86.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (!post.imgUrl.isNullOrEmpty()) {
                    val imageUrlWithCacheBuster = remember(post.imgUrl) {
                        if (post.imgUrl != null) "${toFullUrl(post.imgUrl)}?t=${System.currentTimeMillis()}" else null
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrlWithCacheBuster)
                            .crossfade(true)
                            .size(300, 300) // 서버 대신 앱에서 메모리/디스크 크기에 맞춰 리사이징
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "썸네일",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Image, null, tint = Color.LightGray)
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(text = post.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = post.nickname, fontSize = 12.sp, color = Color(0xFF444444))
                    Text(text = " • ", color = TextSub)
                    Text(text = UtilTime.formatRelativeTime(post.created_at), fontSize = 12.sp, color = TextSub)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        post.tags?.take(2)?.forEach { tag -> Text(text = "#$tag", fontSize = 11.sp, color = Color(0xFF1976D2)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReactionItem(Icons.Default.FavoriteBorder, post.likeCount.toString())
                        ReactionItem(Icons.Outlined.ChatBubbleOutline, post.commentCount.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun ReactionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Text(text = count, fontSize = 11.sp, color = Color.Gray)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun FeedScreenPreview() {
    val dummyPosts = listOf(Post(id="1", category="후기", title="테스트", content="..", nickname="테스터", created_at="2024-01-01", imgUrl=null))
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            // ✅ Preview 에러 방지: dummyPosts 사용
            items(dummyPosts) { PostCard(it, {}) }
        }
    }
}