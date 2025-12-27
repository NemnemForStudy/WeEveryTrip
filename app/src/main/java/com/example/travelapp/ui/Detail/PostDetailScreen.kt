package com.example.travelapp.ui.Detail // 패키지명 확인

import androidx.compose.ui.ExperimentalComposeUiApi
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.travelapp.BuildConfig
import com.example.travelapp.R
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.data.model.ShareUtil
import com.example.travelapp.data.model.ShareUtil.uploadMapCapture
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.util.AnimatedPolyline
import com.example.travelapp.util.UtilTime
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.CameraPositionState
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapEffect
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.PolylineOverlay
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.MarkerIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

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
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentContent by viewModel.commentContent.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val routePoints by viewModel.routePoints.collectAsStateWithLifecycle()
    val userToken = remember { viewModel.getUserToken() }
    val isCapturing by remember { mutableStateOf(false) }

    // 화면 진입 시 딱 한 번 실행 (데이터 요청)
    LaunchedEffect(postId) {
        viewModel.fetchPostDetail(postId)
        viewModel.loadLikeData(postId)
        viewModel.loadComments(postId)
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
//                val orderedLocations = remember(post!!.id) {
//                    post!!.imageLocations.mapNotNull { loc ->
//                        val lat = loc.latitude
//                        val lng = loc.longitude
//                        if(lat != null && lng != null) RoutePoint(latitude = lat, longitude = lng) else null
//                    }.let { list ->
//                        if(list.isNotEmpty()) list
//                        else {
//                            val lat = post!!.latitude
//                            val lng = post!!.longitude
//                            if(lat != null && lng != null) listOf(RoutePoint(latitude = lat, longitude = lng))
//                            else emptyList()
//                        }
//                    }
//                }
                val routePointsByDay by viewModel.routePointsByDay.collectAsState()
                val currentDayIndex by viewModel.currentDayIndex.collectAsState()

                LaunchedEffect(post!!.id, routePointsByDay, currentDayIndex) {
                    val dayKeys = routePointsByDay.keys.sorted()
                    val selectedPoints = dayKeys
                        .getOrNull(currentDayIndex)
                        ?.let{ routePointsByDay[it] } ?: emptyList()
                    if(selectedPoints.size >= 2) {
                        viewModel.fetchRoute(selectedPoints)
                    } else {
                        viewModel.clearRoute()
                    }
                }

                val isMyPost = post!!.userId == currentUserId
                var triggerSnapshot by remember { mutableStateOf(false) }

                PostDetailContent(
                    post = post!!,
                    routePoints = routePoints,
                    routePointsByDay = routePointsByDay,
                    currentDayIndex = currentDayIndex,
                    isLiked = isLiked,
                    likeCount = likeCount,
                    comments = comments,
                    commentInput = commentContent,
                    currentUserId = currentUserId,
                    onLikeToggle = { viewModel.toggleLike(postId) },
                    onCommentChange = { viewModel.updateCommentInput(it) },
                    onCommentSend = { viewModel.createComment(postId) },
                    onCommentEdit = { comment, newContent ->
                        viewModel.updateComment(comment.commentId, newContent)
                    },
                    onCommentDelete = { commentId ->
                        viewModel.deleteComment(commentId)
                    },
                    onBackClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onPostEdit = {
                        navController.navigate("edit/${postId}")
                    },
                    onPostDelete = {
                        viewModel.deletePost(
                            onSuccess = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("post_deleted", true)

                                navController.popBackStack()
                            },
                            onFailure = { }
                        )
                    },
                    onPrevDay = { viewModel.goToPrevDay() },
                    onNextDay = { viewModel.goToNextDay() },
                    onDaySelect = { index -> viewModel.setDayIndex(index) },
                    isMyPost = isMyPost,
                    triggerSnapshot = triggerSnapshot,
                    onSharedClick = { triggerSnapshot = true },
                    onSnapshotDone = { triggerSnapshot = false },
                    userToken = userToken
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
    routePoints: List<RoutePoint>,
    routePointsByDay: Map<Int, List<RoutePoint>>,
    currentDayIndex: Int,
    isLiked: Boolean,
    likeCount: Int,
    comments: List<Comment>,
    commentInput: String,
    currentUserId: String,
    onLikeToggle: () -> Unit,
    onCommentChange: (String) -> Unit,
    onCommentSend: () -> Unit,
    onCommentEdit: (Comment, String) -> Unit,
    onCommentDelete: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onPostEdit: () -> Unit = {},
    onPostDelete: () -> Unit = {},
    onPrevDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onDaySelect: (Int) -> Unit = {},
    isMyPost: Boolean = false,
    triggerSnapshot: Boolean = false,
    onSharedClick: () -> Unit = {},
    onSnapshotDone: () -> Unit = {},
    userToken: String?
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    var hasHeader = post.images?.isNotEmpty() == true || post.imageLocations.isNotEmpty()
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if(showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("여행기 공유", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShareOptionItem(
                        label = "카카오톡",
                        iconRes = R.drawable.kakao_icon, // 카톡 아이콘 이미지 넣어야함.
                        backgroundColor = Color(0xFFFEE500),
                        onClick = {
                            showShareSheet = false
                            onSharedClick()
                        }
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
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
                    IconButton(onClick = {
                        showShareSheet = true
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "공유", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            /*
            CommentInputBar(
                value = commentInput,
                onValueChange = onCommentChange,
                onSendClick = onCommentSend,
                isLiked = isLiked,
                likeCount = likeCount,
                onLikeClick = onLikeToggle
            )
            */
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(top = if(!hasHeader) paddingValues.calculateTopPadding() else 0.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if(hasHeader) {
                item {
                    PostMapHeader(
                        post = post,
                        routePoints = routePoints,
                        routePointsByDay = routePointsByDay,
                        currentDayIndex = currentDayIndex,
                        onPrevDay = onPrevDay,
                        onNextDay = onNextDay,
                        triggerSnapshot = triggerSnapshot,
                        onSnapshotDone = onSnapshotDone,
                        token = userToken
                    )
                }

                item {
                    val dayKeys = routePointsByDay.keys.sorted()
                    val totalDays = dayKeys.size
                    if (totalDays > 0) {
                        val canPrev = currentDayIndex > 0
                        val canNext = currentDayIndex < totalDays - 1
                        val dateLabel = remember(post.travelStartDate, currentDayIndex) {
                            formatTripDateLabel(post.travelStartDate, currentDayIndex)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(top = 10.dp, bottom = 18.dp)
                                .zIndex(2f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onDaySelect(currentDayIndex - 1) }, enabled = canPrev) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "이전 날짜",
                                    tint = if (canPrev) TextDark else TextGray.copy(alpha = 0.35f)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dateLabel ?: "Day ${dayKeys.getOrNull(currentDayIndex) ?: (currentDayIndex + 1)}",
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            IconButton(onClick = { onDaySelect(currentDayIndex + 1) }, enabled = canNext) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "다음 날짜",
                                    tint = if (canNext) TextDark else TextGray.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }
            } else {
                item { Spacer(modifier = Modifier.height(16.dp))}
            }

            item {
                PostBodySection(
                    post = post,
                    likeCount = likeCount,
                    commentCount = comments.size,
                    routePointsByDay = routePointsByDay,
                    currentDayIndex = currentDayIndex,
                    isMyPost = isMyPost,
                    onEditClick = onPostEdit,
                    onDeleteClick = onPostDelete,
                    isHeaderPresent = hasHeader
                )
            }

            /*
            item {
                HorizontalDivider(thickness = 8.dp, color = LightGrayBg)
            }

            item {
                Text(
                    text = "댓글 ${comments.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
                )
            }

            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("첫 번째 댓글을 남겨보세요!", color = Color.Gray)
                    }
                }
            } else {
                items(comments) { comment ->
                    CommentItem(
                        comment = comment,
                        currentUserId = currentUserId,
                        onEditClick = { selectedComment ->
                            commentToEdit = selectedComment
                            showEditDialog = true
                        },
                        onDeleteClick = { commentToDelete ->
                            onCommentDelete(commentToDelete.commentId)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
            */
        }
    }

    if (showEditDialog && commentToEdit != null) {
        EditCommentDialog(
            comment = commentToEdit!!,
            onDismiss = {
                showEditDialog = false
                commentToEdit = null
            },
            onConfirm = { newContent ->
                onCommentEdit(commentToEdit!!, newContent)
                showEditDialog = false
                commentToEdit = null
            }
        )
    }
}

data class MarkerItem(
    val position: LatLng,
    val imageUrl: String? // post.imageLocations의 image_url
)

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun PostMapHeader(
    post: Post,
    routePoints: List<RoutePoint>,
    routePointsByDay: Map<Int, List<RoutePoint>>,
    currentDayIndex: Int,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    triggerSnapshot: Boolean,
    onSnapshotDone: () -> Unit,
    token: String?
) {
    // 현재 선택된 Day 번호 계산
    val dayKeys = routePointsByDay.keys.sorted()
    val currentDayNumber = dayKeys.getOrNull(currentDayIndex)
    val context = LocalContext.current // SharedUtil 실행 위해 필요.
    val scope = rememberCoroutineScope()

    // 1) 서버가 내려준 "사진별 좌표(image_locations)"로 마커 여러개 표시
    // 2) 없으면(또는 전부 GPS가 없으면) post.coordinate(대표 좌표) 1개로 fallback
    val markerItems: List<MarkerItem> = remember(post.imageLocations, currentDayNumber) {
        post.imageLocations
            .filter { it.dayNumber == currentDayNumber }  // 현재 Day만 필터링
            .mapNotNull { loc ->
                val lat = loc.latitude
                val lng = loc.longitude
                if (lat != null && lng != null) {
                    MarkerItem(
                        position = LatLng(lat, lng),
                        imageUrl = loc.imageUrl
                    )
                } else null
            }
    }

    val routeLatLngs = remember(routePoints) {
        routePoints.map { LatLng(it.latitude, it.longitude) }
    }

    val simplifiedRoute = remember(routeLatLngs) {
        simplifyRoute(routeLatLngs, maxPoints = 200)
    }

    // 실제로 그릴 폴리라인 좌표 목록
    val polylineCoords = remember(simplifiedRoute) {
        if(simplifiedRoute.size >= 2) simplifiedRoute else emptyList()
    }

    val pauseIndices = remember(simplifiedRoute, markerItems) {
        if(simplifiedRoute.size < 2 || markerItems.size < 2) emptySet()
        else {
            // 출발 도착 제외한 중간 사진 마커들의 위치에서 멈춤
            markerItems.drop(1).dropLast(1)
                .map { nearestIndex(simplifiedRoute, it.position) }
                .toSet()
        }
    }

    val pointsToShow = if(simplifiedRoute.size >= 2) simplifiedRoute else markerItems.map { it.position }
    val totalDays = dayKeys.size

    if(pointsToShow.isNotEmpty()) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition(pointsToShow.first(), 14.0)
        }

        val view = LocalView.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(Color.LightGray)
        ) {
            if (totalDays > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val canPrev = currentDayIndex > 0
                    val canNext = currentDayIndex < totalDays - 1

                    IconButton(onClick = onPrevDay, enabled = canPrev) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "이전 날짜",
                            tint = if (canPrev) PrimaryBlue else TextGray.copy(alpha = 0.4f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Day ${currentDayIndex + 1} / $totalDays",
                            color = TextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = currentDayNumber?.let { "여행 ${it}일차" } ?: "단일 여행일",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onNextDay, enabled = canNext) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "다음 날짜",
                            tint = if (canNext) PrimaryBlue else TextGray.copy(alpha = 0.4f)
                        )
                    }
                }

                if (totalDays > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(totalDays) { index ->
                            val isSelected = index == currentDayIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) PrimaryBlue
                                        else PrimaryBlue.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            if(pointsToShow.isNotEmpty()) {
                val cameraPositionState = rememberCameraPositionState {
                    // 기본 카메라는 첫 번째 포인트 기준
                    position = CameraPosition(pointsToShow.first(), 14.0)
                }

                val isMapMoving by remember {
                    derivedStateOf { cameraPositionState.isMoving }
                }

                val density = LocalDensity.current
                val view = LocalView.current

                var polylinePlayKey by remember(post.id, currentDayIndex) { mutableStateOf(0) }

                LaunchedEffect(currentDayIndex, pointsToShow) {
                    // route가 준비된 첫 순간에만 카메라 fit + 애니 시작
                    if(pointsToShow.isNotEmpty()) {
                        val cameraUpdate = if (pointsToShow.size == 1) {
                            // 사진이 1장인 경우 해당 좌표로 단순 이동
                            CameraUpdate.scrollTo(pointsToShow.first())
                                .animate(CameraAnimation.Easing, 1000)
                        } else {
                            if(polylineCoords.isNotEmpty()) {
                                val builder = LatLngBounds.Builder()
                                polylineCoords.forEach { builder.include(it) }

                                val bounds = builder.build()
                                val paddingPx = with(density) { 64.dp.roundToPx() }
                                CameraUpdate.fitBounds(bounds, paddingPx)
                                    .animate(CameraAnimation.Fly, 1200)
                            } else {
                                // 폴리라인은 없지만 마커 등 다른 포인트가 있다면 그것으로 대체하거나 무시
                                CameraUpdate.scrollTo(pointsToShow.first())
                                    .animate(CameraAnimation.Easing, 1000)
                            }
                        }

                        cameraPositionState.animate(cameraUpdate)

                        delay(1200)
                        polylinePlayKey = 1
                    }
                }

                val mapUiSettings = remember(triggerSnapshot) {
                    MapUiSettings(
                        isZoomControlEnabled = !triggerSnapshot, // 캡쳐중 일때만 버튼 숨김
                        isCompassEnabled = false,
                        isLogoClickEnabled = false,
                        isLocationButtonEnabled = false // 공유 이미지에는 내 위치 버튼도 없애기.
                    )
                }

                NaverMap(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    try {
                                        view.parent?.requestDisallowInterceptTouchEvent(true)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        },
                    uiSettings = remember(triggerSnapshot) {
                        MapUiSettings(
                            isZoomControlEnabled = !triggerSnapshot,
                            isCompassEnabled = false,
                            isLogoClickEnabled = false,
                            isLocationButtonEnabled = false,
                            isScrollGesturesEnabled = true
                        )
                    },
                    cameraPositionState = cameraPositionState,
                ) {

                    // 만약 공유 버튼 신호가 true라면 지도 찍음.
                    MapEffect(key1 = triggerSnapshot) { map ->
                        if(triggerSnapshot) {
                            Toast.makeText(context, "공유 이미지를 생성 중입니다!", Toast.LENGTH_SHORT).show()
                            map.takeSnapshot { bitmap ->
                                scope.launch {
                                    try {
                                        if (token != null) {
                                            // 1. 서버 업로드 (토큰 포함)
                                            val mapImageUrl = uploadMapCapture(context, bitmap, token)
                                            android.util.Log.d("SHARE_DEBUG", "Post 데이터: $post")
                                            android.util.Log.d("SHARE_DEBUG", "받아온 이미지 URL: $mapImageUrl")
                                            android.util.Log.d("SHARE_DEBUG", "서버 응답 URL: $mapImageUrl")

                                            if(post != null && mapImageUrl != null) {
                                                val sharePost = post.copy(imgUrl = mapImageUrl)
                                                ShareUtil.shareToKakao(context, sharePost)
                                            } else {
                                                Toast.makeText(context, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "로그인 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "공유 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        onSnapshotDone() // ✅ 상태 리셋
                                    }
                                }
                            }
                        }
                    }

                    if (triggerSnapshot && polylineCoords.size >= 2) {
                        PolylineOverlay(
                            coords = polylineCoords,
                            color = Color(0xFF21B6FF),
                            width = 8.dp,
                            zIndex = 10
                        )
                    }

                    // route 준비 후 1회만 애니메이션 실행
                    if(!cameraPositionState.isMoving) {
                        if (simplifiedRoute.size >= 2 && polylinePlayKey > 0) {
                            key(polylinePlayKey) {
                                val stepDelay = (2500L / polylineCoords.size.coerceAtLeast(1))
                                    .coerceIn(3L, 12L)

                                AnimatedPolyline(
                                    coords = polylineCoords,
                                    color = Color.Black.copy(alpha = 0.55f),
                                    width = 14.dp,
                                    zIndex = 1,
                                    stepDelayMs = stepDelay,
                                    pauseIndices = pauseIndices,
                                    pauseDelayMs = 300L
                                )

                                AnimatedPolyline(
                                    coords = polylineCoords,
                                    color = Color(0xFF21B6FF),
                                    width = 8.dp,
                                    zIndex = 2,
                                    stepDelayMs = stepDelay,
                                    pauseIndices = pauseIndices,
                                    pauseDelayMs = 300L
                                )
                            }
                        }
                    }


                    if (polylineCoords.size >= 2) {
                        PolylineArrowMarkers(
                            coords = polylineCoords,
                            color = Color(0xFF21B6FF),
                            zIndex = 3,
                            isVisible = !isMapMoving
                        )
                    }

                    // 마커 여러개 표시
                    markerItems.forEachIndexed { index, item ->
                        val icon = rememberPhotoMarkerIcon(item.imageUrl, sizePx = 160) // 사진은 조금 작게도 OK
                        // MarkerState를 remember로 묶어서 드래그 시 좌표 재계산 방지
                        val state = remember(item.position) { MarkerState(position = item.position) }

                        Marker(
                            state = state,
                            // [최적화] 이동 중에는 캡션 텍스트도 숨겨서 GPU 부하를 줄임
                            captionText = if (isMapMoving) "" else when {
                                markerItems.size == 1 -> ""
                                index == 0 -> "출발"
                                index == markerItems.size - 1 -> "도착"
                                else -> "$index"
                            },
                            icon = icon ?: MarkerIcons.BLUE,
                            anchor = Offset(0.5f, 1.0f)
                        )
                    }
                    if (polylineCoords.size >= 2) {
                        PolylineOverlay(coords = polylineCoords, color = Color(0xFF21B6FF), width = 6.dp)
                    }
                }
            } else {
                MapEmptyPlaceholder()
            }
        }
    }
}

private fun openNaverMapRoute(context: Context, points: List<LatLng>) {
    if(points.size < 2) return

    val start = points.first()
    val end = points.last()

    // 경유지는 가운데
    val via = if(points.size > 2) points.subList(1, points.size - 1) else emptyList()

    fun enc(start: String) = URLEncoder.encode(start, "UTF-8")

    val appUri = buildString {
        append("nmap://route/car?")
        append("slat=${start.latitude}&slng=${start.longitude}&sname=${enc("출발")}")
        append("&dlat=${end.latitude}&dlng=${end.longitude}&dname=${enc("도착")}")

        // v1, v2 형태로 경유지 넣기
        via.forEachIndexed { idx, p ->
            val n = idx + 1
            append("&v${n}lat=${p.latitude}&v${n}lng=${p.longitude}&v${n}name=${enc("여행지 ${n}")}")
        }

        append("&appname=${context.packageName}")
    }

    // 앱 설치 되어있으면 앱으로 열기
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUri))

    // 앱이 없으면 웹으로 fallback
    val webUri = Uri.parse("https://map.naver.com")

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "네이버지도 앱이 없어 내 지도에서 표시합니다.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostImageHeader(post: Post) {
    // 대형 이미지 헤더
    val imageList = (post.images ?: emptyList()).ifEmpty { listOfNotNull(post.imgUrl) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { if(imageList.isNotEmpty()) imageList.size else 1 } // 큰 수
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .background(Color.LightGray)
    ) {
        if (imageList.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val fullUrl = toFullUrl(imageList[page])
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
            // 페이지 인디케이터
            if (imageList.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(imageList.size) { index ->
                        val isSelected = pagerState.currentPage === index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White
                                    else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
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
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun PostBodySection(
    post: Post,
    likeCount: Int = 0,
    commentCount: Int = 0,
    routePointsByDay: Map<Int, List<RoutePoint>>,
    currentDayIndex: Int,
    isMyPost: Boolean = false,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isHeaderPresent: Boolean = true
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if(isHeaderPresent) {
                    it.offset(y = (-20).dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                }else {
                    it // 헤더 없으면 평범하게 시작
                }
            }
            .background(Color.White)
            .padding(20.dp)
    ) {

        val context = LocalContext.current

        val routePoints: List<LatLng> =
            post.imageLocations.mapNotNull { loc ->
                val lat = loc.latitude
                val lng = loc.longitude
                if (lat != null && lng != null) LatLng(lat, lng) else null
            }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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

            Spacer(modifier = Modifier.weight(1f))

            if(isHeaderPresent) {
                Button(
                    onClick = { openNaverMapRoute(context, routePoints) },
                    enabled = routePoints.size >= 2,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        disabledContainerColor = PrimaryBlue.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("길찾기", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = post.nickname, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Text(
                        text = UtilTime.formatRelativeTime(post.created_at),
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }

            // 본인 게시물일 때만 점 세개 표시
            if (isMyPost) {
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = {
                                isMenuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", color = Color.Red) },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color(0xFFE0E0E0))

        Text(
            text = post.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = post.content,
            fontSize = 16.sp,
            color = Color(0xFF444444),
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        if(post.tags != null && post.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                post.tags.forEach { tag ->
                    Text(
                        text = "#$tag",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String,
    onEditClick: (Comment) -> Unit,
    onDeleteClick: (Comment) -> Unit
) {
    // 메뉴가 펼쳐졌는지 여부 저장
    var isMenuExpanded by remember { mutableStateOf(false) }

    // 내 댓글인지 확인 (내 댓글일때만 점 3개가 보임)
    // 서버에서 오는 ID 타입
    val isMyContent = comment.userId.toString() == currentUserId

    Column{
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon (
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Color.LightGray
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column (
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text (
                        text = comment.nickname ?: "알 수 없음",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 날짜
                    Text(
                        text = comment.createdAt.take(10),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.content,
                    fontSize = 15.sp,
                    color = TextDark,
                    lineHeight = 20.sp
                )
            }

            if(isMyContent) {
                Box {
                    IconButton (
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-4).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.Gray
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = {
                                isMenuExpanded = false
                                onEditClick(comment)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("삭제", color = Color.Red) },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteClick(comment)
                            }
                        )
                    }
                }
            }
        }
        Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(top = 12.dp))
    }
}

/**
 * 댓글 입력창 + 좋아요 버튼(하단 고정)
 */
@Composable
fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit
) {
    Surface(
        shadowElevation = 16.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좋아요 버튼 (왼쪽)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                IconButton(onClick = onLikeClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                }
                Text(text = "$likeCount", fontSize = 12.sp, color = Color.Gray)
            }

            // 입력창 (중간)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("댓글을 입력하세요...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = PrimaryBlue
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 전송 버튼 (오른쪽)
            IconButton(
                onClick = onSendClick,
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (value.isNotBlank()) PrimaryBlue else Color.LightGray,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 핀마커 사진 넣기
private fun circleCrop(src: Bitmap, size: Int): Bitmap {
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val scaled = Bitmap.createScaledBitmap(src, size, size, true)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    val r = size / 2f
    canvas.drawCircle(r, r, r, paint)

    val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        color = android.graphics.Color.parseColor("#03C75A")
        strokeWidth = size * 0.08f
    }
    canvas.drawCircle(r, r, r - stroke.strokeWidth / 2f, stroke)

    return output
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
private fun PolylineArrowMarkers(
    coords: List<LatLng>,
    color: Color,
    zIndex: Int,
    isVisible: Boolean
) {
    if (!isVisible || coords.size < 2) return // 이동 중이면 아예 계산 안 함

    val step = remember(coords.size) {
        when {
            coords.size <= 60 -> 4
            coords.size <= 120 -> 6
            else -> 10
        }
    }

    // 2) 좌표/각도(bearing) 목록 계산 (끝점 제외)
    val arrows = remember(coords, step) {
        buildList {
            var i = step
            while (i < coords.size - 1) {
                val a = coords[i]
                val b = coords[i + 1]
                add(ArrowData(a, bearingDeg(a, b)))
                i += step
            }
        }
    }

    // 3) 화살표 마커 렌더링
    //    - Marker 회전(angle) 지원이 불확실해서, 비트맵 자체를 회전해서 icon으로 넣는다.
    arrows.forEach { data ->
        val markerState = remember(data.position) { MarkerState(position = data.position) }
        val icon = remember(data.angleDeg, color) {
            OverlayImage.fromBitmap(createRotatedArrowBitmap(data.angleDeg, color))
        }

        Marker(
            state = MarkerState(position = data.position),
            icon = icon,
            anchor = Offset(0.5f, 0.5f),
            zIndex = zIndex,
            isIconPerspectiveEnabled = false // 성능 최적화
        )
    }
}

private data class ArrowData(
    val position: LatLng,
    val angleDeg: Float
)

private fun bearingDeg(a: LatLng, b: LatLng): Float {
    // 화면 좌표계가 아니라 지리 좌표계 기반의 간단 bearing(근거리용)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
    val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
            kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)
    val brng = Math.toDegrees(kotlin.math.atan2(y, x))
    // 우리가 만든 기본 화살표 비트맵은 "위쪽"을 향하도록 그릴 거라서,
    // bearing(북=0도) 기준으로 그대로 회전시키면 된다.
    return ((brng + 360.0) % 360.0).toFloat()
}

private fun createRotatedArrowBitmap(angleDeg: Float, color: Color): Bitmap {
    val size = 28
    val base = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(base)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }

    // 위쪽을 향하는 삼각형 화살표(기본)
    val path = android.graphics.Path().apply {
        moveTo(size / 2f, size * 0.10f)
        lineTo(size * 0.82f, size * 0.82f)
        lineTo(size / 2f, size * 0.66f)
        lineTo(size * 0.18f, size * 0.82f)
        close()
    }
    canvas.drawPath(path, paint)

    // 회전 적용 (중심 기준)
    val matrix = Matrix().apply {
        postRotate(angleDeg, size / 2f, size / 2f)
    }
    return Bitmap.createBitmap(base, 0, 0, size, size, matrix, true)
}

@Composable
private fun rememberPhotoMarkerIcon(imageUrl: String?, sizePx: Int = 96): OverlayImage? {
    val context = LocalContext.current

    return produceState<OverlayImage?>(initialValue = null, key1 = imageUrl, key2 = sizePx) {
        if(imageUrl.isNullOrBlank()) {
            value = null
            return@produceState
        }

        val fullUrl = toFullUrl(imageUrl) ?: return@produceState

        val request = ImageRequest.Builder(context)
            .data(fullUrl)
            .size(sizePx)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        value = if(result is SuccessResult) { // 로딩 결과가 성공인지 확인
            val bmp = result.drawable.toBitmap() // 받아온 그림 비트맵 데이터로 변환
            OverlayImage.fromBitmap(makeBubbleMarkerBitmap(bmp, sizePx = sizePx)) // 비트맵 동그랗게 깎고 지도용 객체로 변환
        } else null
    }.value
}

// 말풍선(버블) 마커 비트맵
private fun makeBubbleMarkerBitmap(
    photo: Bitmap,
    sizePx: Int
): Bitmap {
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val green = android.graphics.Color.parseColor("#03C75A")
    val borderW = kotlin.math.max(1.5f, sizePx * 0.010f) + 1f
    val outer = borderW / 2f

    val cx = sizePx / 2f

    // 버블(말풍선) Path: 둥근 사각형 + 꼬리
    val tailHeight = sizePx * 0.14f
    val sideInset = kotlin.math.max(2f, sizePx * 0.03f)
    val left = outer + sideInset
    val top = outer
    val right = sizePx - outer - sideInset
    val bottom = sizePx - outer - tailHeight
    val corner = sizePx * 0.12f
    val bubbleRect = android.graphics.RectF(left, top, right, bottom)

    val tailHalf = sizePx * 0.10f
    val tailTipY = sizePx - outer

    val bubblePath = android.graphics.Path().apply {
        addRoundRect(bubbleRect, corner, corner, android.graphics.Path.Direction.CW)
        moveTo(cx - tailHalf, bottom)
        quadTo(cx - tailHalf * 0.25f, bottom + tailHeight * 0.65f, cx, tailTipY)
        quadTo(cx + tailHalf * 0.25f, bottom + tailHeight * 0.65f, cx + tailHalf, bottom)
        close()
    }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.STROKE
        strokeWidth = borderW
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    canvas.drawPath(bubblePath, fillPaint)
    canvas.drawPath(bubblePath, strokePaint)

    // 사진(네모 라운드) - 버블 내부에 넉넉하게 배치
    val inset = borderW + kotlin.math.max(1f, sizePx * 0.008f)
    val photoMaxW = bubbleRect.width() - 2f * inset
    val photoMaxH = bubbleRect.height() - 2f * inset
    val photoSize = kotlin.math.min(photoMaxW, photoMaxH).toInt().coerceAtLeast(1)
    val photoCorner = photoSize * 0.12f

    val photoLeft = cx - photoSize / 2f
    val photoTop = (top + bottom) / 2f - photoSize / 2f

    val photoSquare = squareCropRounded(photo, photoSize, photoCorner)
    canvas.drawBitmap(photoSquare, photoLeft, photoTop, null)

    // 사진 테두리(초록 얇게)
    val photoBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.STROKE
        strokeWidth = borderW
    }
    val photoRect = android.graphics.RectF(
        photoLeft + photoBorder.strokeWidth / 2f,
        photoTop + photoBorder.strokeWidth / 2f,
        photoLeft + photoSize - photoBorder.strokeWidth / 2f,
        photoTop + photoSize - photoBorder.strokeWidth / 2f
    )
    canvas.drawRoundRect(photoRect, photoCorner, photoCorner, photoBorder)

    return output
}

private fun squareCropRounded(src: Bitmap, size: Int, cornerRadiusPx: Float): Bitmap {
    val minDim = kotlin.math.min(src.width, src.height)
    val x = (src.width - minDim) / 2
    val y = (src.height - minDim) / 2
    val cropped = Bitmap.createBitmap(src, x, y, minDim, minDim)
    val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)

    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
    return output
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

private fun formatTripDateLabel(travelStartDate: String?, dayIndex: Int): String? {
    if (travelStartDate.isNullOrBlank()) return null
    return try {
        val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
        val outFmt = java.text.SimpleDateFormat("MM/dd", java.util.Locale.KOREA)
        val start = inFmt.parse(travelStartDate) ?: return null
        val cal = java.util.Calendar.getInstance()
        cal.time = start
        cal.add(java.util.Calendar.DAY_OF_MONTH, dayIndex)
        outFmt.format(cal.time)
    } catch (_: Exception) {
        null
    }
}

fun nearestIndex(path: List<LatLng>, target: LatLng): Int {
    var bestIdx = 0
    var best = Double.MAX_VALUE
    for(i in path.indices) {
        val dLat = path[i].latitude - target.latitude
        val dLng = path[i].longitude - target.longitude
        val dist = dLat * dLat + dLng * dLng
        if(dist < best) {
            best = dist
            bestIdx = i
        }
    }
    return bestIdx
}

fun simplifyRoute(points: List<LatLng>, maxPoints: Int): List<LatLng> {
    if (points.size <= maxPoints) return points
    if (maxPoints <= 2) return listOf(points.first(), points.last())

    val step = points.size.toDouble() / (maxPoints - 1)
    val simplified = mutableListOf<LatLng>()
    var accumulated = 0.0
    while (accumulated < points.size - 1) {
        simplified += points[accumulated.toInt()]
        accumulated += step
    }
    simplified += points.last()
    return simplified
}

@Composable
fun MapEmptyPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)), // 연한 회색 배경
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            // LocationOn 대신 LocationOff 아이콘을 쓰면 더 직관적입니다.
            // 아이콘이 없다면 Icons.Default.LocationOn을 사용하세요.
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "해당 날짜에 사진을 추가해주세요!",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ShareOptionItem(
    label: String,
    iconRes: Int,              // 🔥 리소스 ID를 직접 받음
    backgroundColor: Color,
    tint: Color = Color.Unspecified, // 로고 색상을 유지하기 위해 기본값 Unspecified
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            color = backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = tint, // 아이콘 고유 색상을 쓰려면 Unspecified, 아니면 특정 색상 지정
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
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

