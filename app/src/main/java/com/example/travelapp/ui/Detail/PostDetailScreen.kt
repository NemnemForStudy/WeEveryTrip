package com.example.travelapp.ui.Detail

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.travelapp.R
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.data.model.ShareUtil
import com.example.travelapp.data.model.ShareUtil.uploadMapCapture
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.util.*
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.naver.maps.map.util.MarkerIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ShareTarget { KAKAO, INSTAGRAM }

val PrimaryBlue = Color(0xFF4A90E2)
val TextDark = Color(0xFF222222)

@RequiresApi(Build.VERSION_CODES.O)
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
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val routePoints by viewModel.routePoints.collectAsStateWithLifecycle()
    val userToken = remember { viewModel.getUserToken() }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }

    LaunchedEffect(postId) {
        viewModel.fetchPostDetail(postId)
        viewModel.loadLikeData(postId)
        viewModel.loadComments(postId)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            errorMsg != null -> Text(text = errorMsg ?: "오류 발생", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            post != null -> {
                val routePointsByDay by viewModel.routePointsByDay.collectAsState()
                val currentDayIndex by viewModel.currentDayIndex.collectAsState()

                LaunchedEffect(post!!.id, routePointsByDay, currentDayIndex) {
                    val dayKeys = routePointsByDay.keys.sorted()
                    val selectedPoints = dayKeys.getOrNull(currentDayIndex)?.let { routePointsByDay[it] } ?: emptyList()
                    if (selectedPoints.size >= 2) viewModel.fetchRoute(selectedPoints) else viewModel.clearRoute()
                }

                PostDetailContent(
                    post = post!!,
                    routePoints = routePoints,
                    routePointsByDay = routePointsByDay,
                    currentDayIndex = currentDayIndex,
                    isLiked = isLiked,
                    likeCount = likeCount,
                    currentUserId = currentUserId,
                    onLikeToggle = { viewModel.toggleLike(postId) },
                    onBackClick = { navController.popBackStack() },
                    onPostEdit = { navController.navigate("edit/${postId}") },
                    onPostDelete = { viewModel.deletePost(onSuccess = { navController.popBackStack() }, onFailure = {}) },
                    onDaySelect = { index -> viewModel.setDayIndex(index) },
                    shareTarget = shareTarget,
                    onSharedClick = { shareTarget = it },
                    onSnapshotDone = { shareTarget = null },
                    userToken = userToken
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PostDetailContent(
    post: Post,
    routePoints: List<RoutePoint>,
    routePointsByDay: Map<Int, List<RoutePoint>>,
    currentDayIndex: Int,
    isLiked: Boolean,
    likeCount: Int,
    currentUserId: String,
    onLikeToggle: () -> Unit,
    onBackClick: () -> Unit,
    onPostEdit: () -> Unit,
    onPostDelete: () -> Unit,
    onDaySelect: (Int) -> Unit,
    shareTarget: ShareTarget?,
    onSharedClick: (ShareTarget) -> Unit,
    onSnapshotDone: () -> Unit,
    userToken: String?
) {
    var showShareSheet by remember { mutableStateOf(false) }
    val hasHeader = post.images?.isNotEmpty() == true || post.imageLocations.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()

    // 1. 마커 데이터 미리 계산
    val dayKeys = routePointsByDay.keys.sorted()
    val currentDayNumber = dayKeys.getOrNull(currentDayIndex)
    val markerItems: List<MarkerItemWithIndex> = remember(post.imageLocations, currentDayNumber) {
        val filtered = post.imageLocations.filter { it.dayNumber == currentDayNumber }
        filtered.mapIndexedNotNull { index, loc ->
            val lat = loc.latitude ?: return@mapIndexedNotNull null
            val lng = loc.longitude ?: return@mapIndexedNotNull null
            MarkerItemWithIndex(LatLng(lat, lng), loc.imageUrl, index + 1, index == 0, index == filtered.size - 1)
        }
    }

    // 2. 하단 리스트 상태 (PagerState)
    val pagerState = rememberPagerState(pageCount = { markerItems.size })

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Surface(modifier = Modifier.padding(8.dp), shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                        IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "뒤로", tint = TextDark) }
                    }
                },
                actions = { IconButton(onClick = { showShareSheet = true }) { Icon(Icons.Default.Share, "공유", tint = Color.Black) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pv ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = pv.calculateBottomPadding())) {
            if (hasHeader) {
                // [지도 헤더]
                item {
                    PostMapHeader(
                        post = post,
                        markerItems = markerItems,
                        routePoints = routePoints,
                        selectedPageIndex = pagerState.currentPage,
                        onMarkerClick = { index ->
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        shareTarget = shareTarget,
                        onSnapshotDone = onSnapshotDone,
                        token = userToken
                    )
                }

                // [하단 장소 카드 Carousel]
                if (markerItems.isNotEmpty()) {
                    item {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 40.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) { page ->
                            LocationCard(markerItems[page])
                        }
                    }
                }

                // [날짜 선택 Row]
                item {
                    if (dayKeys.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onDaySelect(currentDayIndex - 1) }, enabled = currentDayIndex > 0) { Icon(Icons.Default.ChevronLeft, null) }
                            Text(MapUtil.formatTripDateLabel(post.travelStartDate, currentDayIndex) ?: "Day ${currentDayIndex + 1}", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDaySelect(currentDayIndex + 1) }, enabled = currentDayIndex < dayKeys.size - 1) { Icon(Icons.Default.ChevronRight, null) }
                        }
                    }
                }
            }

            // [게시글 본문]
            item { PostBodySection(post, currentUserId == post.userId, onPostEdit, onPostDelete, hasHeader) }

            // [댓글 섹션 주석 처리]
            /*
            item { HorizontalDivider(thickness = 8.dp, color = Color(0xFFF5F7FA)) }
            ... (중략) ...
            */
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }, containerColor = Color.White) {
            Row(Modifier.fillMaxWidth().padding(bottom = 40.dp), Arrangement.SpaceEvenly) {
                ShareOptionItem("카카오톡", R.drawable.kakao_icon, Color(0xFFFFEB3B)) { showShareSheet = false; onSharedClick(ShareTarget.KAKAO) }
                ShareOptionItem("인스타그램", R.drawable.instagram_icon, Color.Transparent) { showShareSheet = false; onSharedClick(ShareTarget.INSTAGRAM) }
            }
        }
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun PostMapHeader(
    post: Post,
    markerItems: List<MarkerItemWithIndex>,
    routePoints: List<RoutePoint>,
    selectedPageIndex: Int,
    onMarkerClick: (Int) -> Unit,
    shareTarget: ShareTarget?,
    onSnapshotDone: () -> Unit,
    token: String?
) {
    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()
    val routeLatLngs = remember(routePoints) { routePoints.map { LatLng(it.latitude, it.longitude) } }

    // 경로 애니메이션용 (전체 좌표를 다 보여주기 위해 take 사용 안 함)
    var animatedProgress by remember(markerItems, routeLatLngs) { mutableStateOf(0) }
    val visibleCoords = remember(routeLatLngs, animatedProgress) { routeLatLngs.take(animatedProgress) }

    // 1. 초기 셋업: 지도가 켜질 때 전체 경로가 다 보이도록 fitBounds
    LaunchedEffect(markerItems, routeLatLngs) {
        val allPoints = (markerItems.map { it.position } + routeLatLngs)
        if (allPoints.isNotEmpty()) {
            if (allPoints.size >= 2) {
                val bounds = LatLngBounds.Builder().apply { allPoints.forEach { include(it) } }.build()
                cameraPositionState.animate(CameraUpdate.fitBounds(bounds, 150))
            } else {
                cameraPositionState.animate(CameraUpdate.scrollAndZoomTo(allPoints.first(), 15.0))
            }

            // 경로 애니메이션 실행
            if (routeLatLngs.size >= 2) {
                animatedProgress = 0
                for (i in 1..50) { animatedProgress = (routeLatLngs.size * (i / 50f)).toInt(); delay(16) }
                animatedProgress = routeLatLngs.size
            }
        }
    }

    // 2. 카드를 넘길 때: 선택된 장소로 카메라 부드럽게 이동
    LaunchedEffect(selectedPageIndex) {
        if (markerItems.isNotEmpty()) {
            val target = markerItems[selectedPageIndex].position
            cameraPositionState.animate(CameraUpdate.scrollTo(target))
        }
    }

    Box(Modifier.fillMaxWidth().height(350.dp)) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(isZoomControlEnabled = true, isLocationButtonEnabled = false, isLogoClickEnabled = false)
        ) {
            // [핵심 로직] 선택된 카드의 마커 딱 하나만 렌더링
            markerItems.getOrNull(selectedPageIndex)?.let { selectedItem ->
                val icon = rememberClusteredPhotoIcon(
                    imageUrl = selectedItem.imageUrl,
                    index = selectedItem.index,
                    count = 1, // 개별 모드이므로 1 고정
                    sizePx = 200,
                    isSelected = true
                )

                Marker(
                    state = MarkerState(position = selectedItem.position),
                    icon = icon ?: MarkerIcons.BLUE,
                    zIndex = 1000,
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                )
            }

            // [경로선] 필터링과 관계없이 항상 전체 경로 노출
            if (visibleCoords.size >= 2) {
                PolylineOverlay(
                    coords = visibleCoords,
                    color = Color(0xFF21B6FF),
                    width = 6.dp,
                    capType = LineCap.Round,
                    joinType = LineJoin.Round
                )
            }

            // 스냅샷 처리...
            MapEffect(key1 = shareTarget) { map ->
                if (shareTarget != null) map.takeSnapshot { bitmap ->
                    scope.launch {
                        try {
                            if (shareTarget == ShareTarget.INSTAGRAM) ShareUtil.shareToInstagramFeed(map.context, bitmap)
                            else if (token != null) uploadMapCapture(map.context, bitmap, token)?.let { ShareUtil.shareToKakao(map.context, post.copy(imgUrl = it)) }
                        } finally { onSnapshotDone() }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationCard(item: MarkerItemWithIndex) {
    Card(
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(32.dp).background(PrimaryBlue, CircleShape), contentAlignment = Alignment.Center) {
                Text("${item.index}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = MapUtil.toFullUrl(item.imageUrl),
                contentDescription = null,
                modifier = Modifier.size(65.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                val title = when {
                    item.isStart -> "여행의 시작"
                    item.isEnd -> "마지막 도착지"
                    else -> "${item.index}번째 방문지"
                }
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text(text = "사진 갤러리 보기", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PostBodySection(post: Post, isMyPost: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, isHeader: Boolean) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).let { if(isHeader) it.offset(y = (-20).dp) else it }
        .background(Color.White, RoundedCornerShape(24.dp)).padding(20.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = PrimaryBlue.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                Text(post.category ?: "일반", color = PrimaryBlue, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            if (isMyPost) {
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("수정") }, onClick = { isMenuExpanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("삭제", color = Color.Red) }, onClick = { isMenuExpanded = false; onDelete() })
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFE0E0E0)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(post.nickname.take(1), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(post.nickname, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(UtilTime.formatRelativeTime(post.created_at), color = Color.Gray, fontSize = 12.sp)
            }
        }

        Divider(Modifier.padding(vertical = 20.dp), color = Color(0xFFEEEEEE))
        Text(post.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
        Spacer(Modifier.height(12.dp))
        Text(post.content, fontSize = 16.sp, color = Color(0xFF444444), lineHeight = 24.sp)

        if (!post.tags.isNullOrEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                post.tags.forEach { Text("#$it", color = PrimaryBlue, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
fun ShareOptionItem(label: String, iconRes: Int, bgColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Surface(Modifier.size(50.dp), shape = CircleShape, color = bgColor, border = if(bgColor == Color.Transparent) BorderStroke(1.dp, Color.LightGray) else null) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), label, Modifier.size(30.dp), tint = Color.Unspecified)
            }
        }
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium)
    }
}