package com.nemnem.travelapp.ui.Detail

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.nemnem.travelapp.R
import com.nemnem.travelapp.data.model.Post
import com.nemnem.travelapp.data.model.RoutePoint
import com.nemnem.travelapp.data.model.ShareUtil
import com.nemnem.travelapp.util.MapUtil
import com.nemnem.travelapp.util.MarkerItemWithIndex
import com.nemnem.travelapp.util.UtilTime
import com.nemnem.travelapp.util.rememberClusteredPhotoIcon
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapEffect
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.PolylineOverlay
import com.naver.maps.map.compose.rememberCameraPositionState
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
    val routePointsByDay by viewModel.routePointsByDay.collectAsState()
    val currentDayIndex by viewModel.currentDayIndex.collectAsState()

    val shouldRefresh by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("should_refresh", false) // 초기값 false
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh == true) {
            Log.d("PostDetail", "수정 완료 신호 감지 -> 데이터 새로고침")
            viewModel.fetchPostDetail(postId)
            // ✅ 신호를 처리했으므로 다시 false로 세팅합니다.
            navController.currentBackStackEntry?.savedStateHandle?.set("should_refresh", false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchPostDetail(postId)
        viewModel.loadLikeData(postId)
    }

    //  post 데이터가 로드된 직후, 첫날 경로 데이터 로드 (한 번만)
    LaunchedEffect(post) { // post 객체 자체가 로드되었을 때 한 번만 실행
        post?.let { p ->
            // 이미 데이터가 로드되어 있다면 중복 요청 방지 (선택 사항)
            if (routePoints.isEmpty()) {
                val dayKeys = p.imageLocations.mapNotNull { it.dayNumber }.distinct().sorted()
                val initialDayNum = dayKeys.firstOrNull()

                if (initialDayNum != null) {
                    val pointsToRoute = p.imageLocations
                        .filter { it.dayNumber == initialDayNum && it.latitude != null && it.longitude != null }
                        .map { RoutePoint(latitude = it.latitude!!, longitude = it.longitude!!) }

                    if (pointsToRoute.size >= 2) {
                        Log.d("PostDetail", "첫날 경로 요청 시작")
                        viewModel.fetchRoute(pointsToRoute)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            errorMsg != null -> Text(text = errorMsg ?: "오류 발생", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            post != null -> {
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
                    onDaySelect = { index ->
                        viewModel.setDayIndex(index)

                        val dayKeys = routePointsByDay.keys.sorted()
                        val selectedDayNum = dayKeys.getOrNull(index)

                        val pointsToRoute = post!!.imageLocations
                            .filter { it.dayNumber == selectedDayNum && it.latitude != null && it.longitude != null }
                            .map { RoutePoint(latitude = it.latitude!!, longitude = it.longitude!!) }

                        if (pointsToRoute.size >= 2) {
                            viewModel.fetchRoute(pointsToRoute)
                        } else {
                            viewModel.clearRoute()
                        }
                    },
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
    val context = LocalContext.current
    var showShareSheet by remember { mutableStateOf(false) }
    val hasHeader = post.images?.isNotEmpty() == true || post.imageLocations.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(shareTarget) {
        if (shareTarget == ShareTarget.KAKAO) {
            // 1. 이미 만들어두신 ShareUtil의 함수를 호출합니다.
            // post.images의 첫번째 사진이나 imgUrl을 넘겨주도록 되어있으므로 post 객체를 통째로 넘깁니다.
            ShareUtil.shareToKakao(context, post)

            // 2. 중요: 공유가 끝나면 target을 다시 null로 바꿔줘야 합니다.
            // 그래야 다음에 또 눌렀을 때 LaunchedEffect가 작동합니다. (이게 안되면 두번짼 안눌림)
            onSnapshotDone()
        }
    }

    //  마커 데이터 계산
    val dayKeys = routePointsByDay.keys.sorted()
    val currentDayNumber = dayKeys.getOrNull(currentDayIndex)
    val markerItems: List<MarkerItemWithIndex> = remember(post.imageLocations, currentDayNumber) {
        val filtered = post.imageLocations.filter { it.dayNumber == currentDayNumber }
        filtered.mapIndexed { index, loc ->
            val position = if (loc.latitude != null && loc.longitude != null) {
                LatLng(loc.latitude!!, loc.longitude!!)
            } else null

            MarkerItemWithIndex(
                position = position,
                imageUrl = loc.imageUrl,
                index = index + 1,
                isStart = index == 0,
                isEnd = index == filtered.size - 1
            )
        }
    }

    // currentDayNumber가 변경될 때마다 pagerState를 재생성
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { markerItems.size }
    )

    // 날짜 변경 시 페이저 초기화
    LaunchedEffect(currentDayNumber) {
        Log.d("PostDetailContent", "currentDayNumber changed: $currentDayNumber, resetting pager to page 0")
        if (markerItems.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    // pagerState.currentPage를 selectedPageIndex로 사용
    LaunchedEffect(pagerState.currentPage) {
        Log.d("PostDetailContent", "pagerState.currentPage changed: ${pagerState.currentPage}")
    }

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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pv)) {
            if (hasHeader) {
                //  지도 헤더
                item {
                    Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
                        PostMapHeader(
                            post = post,
                            markerItems = markerItems,
                            routePoints = routePoints,
                            selectedPageIndex = pagerState.currentPage,
                            onMarkerClick = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            shareTarget = shareTarget,
                            onSnapshotDone = onSnapshotDone,
                            token = userToken,
                            onMapSnapshotCaptured = { mapBitmap -> /* 기존 로직 동일 */ }
                        )
                    }
                }

                //  하단 장소 카드 Carousel
                if (markerItems.isNotEmpty()) {
                    item {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 40.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier.padding(vertical = 10.dp),
                            userScrollEnabled = true,
                            key = { index -> markerItems.getOrNull(index)?.imageUrl ?: index }
                        ) { page ->
                            LocationCard(markerItems[page])
                        }
                    }
                }

                //  날짜 선택 Row
                item {
                    if (dayKeys.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onDaySelect(currentDayIndex - 1) }, enabled = currentDayIndex > 0) { Icon(Icons.Default.ChevronLeft, null) }
                            Text(MapUtil.formatTripDateLabel(post.travelStartDate, currentDayIndex) ?: "Day ${currentDayIndex + 1}", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDaySelect(currentDayIndex + 1) }, enabled = currentDayIndex < dayKeys.size - 1) { Icon(Icons.Default.ChevronRight, null) }
                        }
                    }
                }
            } else {
                item { Spacer(Modifier.height(24.dp)) }
            }

            //  게시글 본문
            item {
                PostBodySection(
                    post = post,
                    isMyPost = currentUserId == post.userId,
                    onEdit = onPostEdit,
                    onDelete = onPostDelete,
                    isHeader = hasHeader // 일단 디자인 유지를 위해 true로 두되, 버튼이 안 눌리면 이걸 false로 바꿔보세요.
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    //  공유 옵션 시트
    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
            Row(Modifier.fillMaxWidth().padding(bottom = 40.dp), Arrangement.SpaceEvenly) {
                ShareOptionItem("카카오톡", R.drawable.kakao_icon, Color(0xFFFFEB3B)) {
                    showShareSheet = false
                    onSharedClick(ShareTarget.KAKAO)
                }
                ShareOptionItem("인스타그램", R.drawable.instagram_icon, Color.White) {
                    showShareSheet = false
                    onSharedClick(ShareTarget.INSTAGRAM)
                }
            }
        }
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun PostMapHeader(
    post: Post,
    markerItems: List<MarkerItemWithIndex>,routePoints: List<RoutePoint>,
    selectedPageIndex: Int,
    onMarkerClick: (Int) -> Unit,
    shareTarget: ShareTarget?,
    onSnapshotDone: () -> Unit,
    token: String?,
    onMapSnapshotCaptured: (Bitmap) -> Unit
) {
    // 1. 카메라 상태 정의 (LaunchedEffect에서 사용하기 위해 상단에 유지)
    val cameraPositionState = rememberCameraPositionState()

    // 2. 경로 데이터
    val routeLatLngs = remember(routePoints) {
        routePoints.map { LatLng(it.latitude, it.longitude) }
    }

    // MapUiSettings를 반드시 remember로 고정
    val uiSettings = remember {
        MapUiSettings(
            isZoomControlEnabled = true,
            isScrollGesturesEnabled = true,
            isZoomGesturesEnabled = true,
            isLocationButtonEnabled = false,
            isRotateGesturesEnabled = false,
            isTiltGesturesEnabled = false
        )
    }

    val isSharingMode = shareTarget == ShareTarget.INSTAGRAM
    val visibleCoords = remember(routeLatLngs) { routeLatLngs }

    LaunchedEffect(selectedPageIndex, markerItems) {
        if (!isSharingMode && markerItems.isNotEmpty()) {
            val targetItem = markerItems.getOrNull(selectedPageIndex)
            targetItem?.position?.let { targetLatLng ->
                // 카메라가 이미 그 위치에 있다면 이동하지 않도록 방어 (부하 감소)
                if (cameraPositionState.position.target != targetLatLng) {
                    cameraPositionState.animate(
                        update = CameraUpdate.scrollTo(targetLatLng),
                        animation = CameraAnimation.Easing,
                        durationMs = 500
                    )
                }
            }
        }
    }

    Box(Modifier.fillMaxWidth().height(350.dp)) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            contentPadding = PaddingValues(top = 10.dp),
            onMapClick = { _, _ -> /* 지도 클릭 시 동작 */ }
        ) {
            // [1] 초기 진입 시 전체 경로가 보이게 이동
            MapEffect(key1 = routeLatLngs) { map ->
                Log.d("PostMapHeader", "MapEffect[routeLatLngs] triggered")
                if (routeLatLngs.size >= 2) {
                    val bounds = LatLngBounds.Builder().apply {
                        routeLatLngs.forEach { include(it) }
                    }.build()
                    map.moveCamera(CameraUpdate.fitBounds(bounds, 200))
                } else if (routeLatLngs.isNotEmpty()) {
                    map.moveCamera(CameraUpdate.scrollTo(routeLatLngs[0]))
                }
            }

            // 마커 그리기
            markerItems.forEachIndexed { index, item ->
                // 공유 모드이거나 현재 선택된 인덱스인 경우 마커 표시 (또는 전체 표시 등 기획에 따라 조절)
                item.position?.let { pos ->
                    val isSelected = index == selectedPageIndex
                    val icon = rememberClusteredPhotoIcon(item.imageUrl, item.index, 1, 100, isSelected)
                    Marker(
                        state = MarkerState(position = pos),
                        icon = icon ?: MarkerIcons.BLUE,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        zIndex = if (isSelected) 100 else 0,
                        onClick = {
                            onMarkerClick(index)
                            true
                        }
                    )
                }
            }

            // 경로 그리기
            if (visibleCoords.size >= 2) {
                PolylineOverlay(
                    coords = visibleCoords,
                    color = Color(0xFF21B6FF),
                    width = 8.dp,
                )
            }

            // 스냅샷
            MapEffect(key1 = shareTarget) { map ->
                if (isSharingMode) {
                    delay(500)
                    map.takeSnapshot { onMapSnapshotCaptured(it) }
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
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .let { if (isHeader) it.offset(y = (-20).dp) else it }
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
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
        Surface(Modifier.size(50.dp), shape = CircleShape, color = bgColor, border = if (bgColor == Color.Transparent) BorderStroke(1.dp, Color.LightGray) else null) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), label, Modifier.size(30.dp), tint = Color.Unspecified)
            }
        }
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium)
    }
}