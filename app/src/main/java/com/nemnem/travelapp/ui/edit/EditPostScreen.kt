package com.nemnem.travelapp.ui.edit

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nemnem.travelapp.ui.components.PostForm
import com.nemnem.travelapp.ui.theme.StandardBlue
import com.nemnem.travelapp.util.AnimatedPolyline
import com.nemnem.travelapp.util.DateUtils
import com.nemnem.travelapp.util.MapUtil.toFullUrl
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.nemnem.travelapp.ui.write.PostImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalNaverMapApi::class)
@Composable
fun EditPostScreen(
    navController: NavController,
    postId: String,
    viewModel: EditPostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 1. ViewModel 상태 관찰
    val title by viewModel.title.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val existingImages by viewModel.images.collectAsStateWithLifecycle()
    val groupedImages by viewModel.groupedImages.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val tripDays by viewModel.tripDays.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val routePoints by viewModel.routePoints.collectAsState()

    // 2. 로컬 UI 상태
    var tagsInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var mapDialogImages by remember { mutableStateOf<List<PostImage>>(emptyList()) }
    var mapDialogTitle by remember { mutableStateOf("") }

    val dateRangePickerState = key(startDate, endDate) {
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate
        )
    }

    LaunchedEffect(startDate, endDate) {
        if (startDate != null && endDate != null) {
            dateRangePickerState.setSelection(startDate, endDate)
        }
    }

    // 3. 게시물 로드 및 결과 처리
    LaunchedEffect(postId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModel.loadPost(postId)
        }
    }

    BackHandler {
        navController.previousBackStackEntry?.savedStateHandle?.set("should_refresh", true)
        navController.popBackStack()
    }

    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            is EditPostViewModel.UpdateStatus.Success -> {
                Toast.makeText(context, "게시물이 수정되었습니다!", Toast.LENGTH_SHORT).show()

                navController.previousBackStackEntry?.savedStateHandle?.set("should_refresh", true)

                viewModel.resetStatus()
                navController.popBackStack()
            }
            is EditPostViewModel.UpdateStatus.Error -> {
                Toast.makeText(context, (updateStatus as EditPostViewModel.UpdateStatus.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    // 4. 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.processSelectedImages(context, uris)
    }

    // --- UI 그리기 ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            PostForm(
                navController = navController,
                drawerState = drawerState,
                title = title,
                onTitleChange = viewModel::updateTitle,
                content = content,
                onContentChange = viewModel::updateContent,
                category = category,
                onCategoryClick = { showCategoryDialog = true },
                tagsInput = tagsInput,
                onTagsChange = { tagsInput = it },
                startDate = startDate,
                endDate = endDate,
                onDateClick = { showDatePicker = true },
                tripDays = tripDays,
                groupedImages = groupedImages,
                existingImages = emptyList(),
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onSwapImages = viewModel::swapImages,
                onPreviewClick = { day, images ->
                    val validImages = images.filter { it.latitude != null && it.longitude != null }

                    if (validImages.isNotEmpty()) {
                        mapDialogImages = validImages
                        mapDialogTitle = "Day $day 위치 미리보기"
                        showMapDialog = true

                        viewModel.fetchRoute(validImages.map { it.latitude!! to it.longitude!! })
                    } else {
                        Toast.makeText(context, "위치 정보가 포함된 사진이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                submitButtonText = "수정",
                isSubmitting = updateStatus is EditPostViewModel.UpdateStatus.Loading,
                onSubmitClick = {
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        viewModel.updatePost(postId, context)
                    } else {
                        Toast.makeText(context, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                toFullUrl = { url -> toFullUrl(url) ?: "" },
                onRemoveImage = { day, image ->
                    viewModel.removeImage(day, image)
                }
            )
        }

        // --- 다이얼로그 모음 ---

        if (showCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                title = { Text("여행 유형 선택") },
                confirmButton = { TextButton(onClick = { viewModel.updateCategory("국내여행"); showCategoryDialog = false }) { Text("국내여행") } },
            )
        }

        if (showDatePicker) {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = StandardBlue,
                onPrimary = Color.White,
                surface = Color.White,
                onSurface = Color.Black,
                secondaryContainer = StandardBlue.copy(alpha = 0.1f)
            )) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateDateRange(
                                dateRangePickerState.selectedStartDateMillis,
                                dateRangePickerState.selectedEndDateMillis
                            )
                            showDatePicker = false
                        }) {
                            Text("확인", fontWeight = FontWeight.Bold, color = StandardBlue)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("취소", color = Color.Gray)
                        }
                    }
                ) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.height(500.dp),
                        title = {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(text = "날짜 선택", style = MaterialTheme.typography.labelLarge)
                            }
                        },
                        headline = {
                            val start = dateRangePickerState.selectedStartDateMillis
                            val end = dateRangePickerState.selectedEndDateMillis

                            val headlineText = if (start != null && end != null) {
                                "${DateUtils.formatToDisplay(start)} - ${DateUtils.formatToDisplay(end)}"
                            } else {
                                "시작일 - 종료일"
                            }

                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = headlineText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    )
                }
            }
        }

        if (showMapDialog && mapDialogImages.isNotEmpty()) {
            Dialog(onDismissRequest = { showMapDialog = false }) {
                Card(modifier = Modifier.fillMaxWidth().height(450.dp), shape = RoundedCornerShape(16.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // ✅ 핵심 수정: NaverMap을 showMapDialog 외부에서 생성
                        // mapDialogImages가 변해도 NaverMap 재생성 안 함
                        MapDialogContent(
                            mapDialogImages = mapDialogImages,
                            mapDialogTitle = mapDialogTitle,
                            routePoints = routePoints,
                            onDismiss = { showMapDialog = false }
                        )
                    }
                }
            }
        }

        if(updateStatus is EditPostViewModel.UpdateStatus.Loading) {
            Dialog(onDismissRequest = { }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = StandardBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "수정 중입니다...",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✅ 분리된 Map Dialog Content
 * 이렇게 분리하면 mapDialogImages 변경 시 NaverMap이 재생성되지 않음
 */
@OptIn(ExperimentalNaverMapApi::class)
@Composable
private fun MapDialogContent(
    mapDialogImages: List<PostImage>,
    mapDialogTitle: String,
    routePoints: List<com.nemnem.travelapp.data.model.RoutePoint>,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val startLat = mapDialogImages[0].latitude!!
        val startLng = mapDialogImages[0].longitude!!
        val startPosition = LatLng(startLat, startLng)

        // ✅ key 제거: NaverMap이 재생성되지 않음
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition(startPosition, 16.0)
        }

        val pagerState = rememberPagerState(pageCount = { mapDialogImages.size })

        LaunchedEffect(pagerState.currentPage) {
            val image = mapDialogImages[pagerState.currentPage]
            val target = LatLng(image.latitude!!, image.longitude!!)

            Log.d("MAP_DEBUG", "🎯 페이저 페이지 변경: ${pagerState.currentPage} -> $target")

            cameraPositionState.animate(
                update = CameraUpdate.scrollAndZoomTo(target, 16.0),
                animation = CameraAnimation.Fly
            )
        }

        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            mapDialogImages.forEachIndexed { i, image ->
                Marker(
                    state = MarkerState(position = LatLng(image.latitude!!, image.longitude!!)),
                    captionText = "${i + 1}",
                    iconTintColor = if (i == pagerState.currentPage) Color.Red else Color.Blue,
                    zIndex = if (i == pagerState.currentPage) 100 else 0,
                    width = if (i == pagerState.currentPage) 40.dp else 30.dp,
                    height = if (i == pagerState.currentPage) 50.dp else 40.dp
                )
            }

            val polyCoords = routePoints.map { LatLng(it.latitude, it.longitude) }
            if (polyCoords.size >= 2) {
                AnimatedPolyline(coords = polyCoords)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.9f))
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(mapDialogTitle, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                val image = mapDialogImages[page]
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${page+1}번\n${image.latitude}, ${image.longitude}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.background(Color.Black.copy(0.5f)).padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}