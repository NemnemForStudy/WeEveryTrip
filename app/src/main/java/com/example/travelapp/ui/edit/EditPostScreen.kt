package com.example.travelapp.ui.edit

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.travelapp.BuildConfig
import com.example.travelapp.ui.components.PostForm
import com.example.travelapp.util.AnimatedPolyline
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.*
import java.util.*

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
    val routePoints by viewModel.routePoints.collectAsStateWithLifecycle()

    // 2. 로컬 UI 상태
    var tagsInput by remember { mutableStateOf("") } // 태그는 필요 시 VM 연동
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var mapDialogLocations by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var mapDialogTitle by remember { mutableStateOf("") }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate,
        initialSelectedEndDateMillis = endDate
    )

    // 3. 게시물 로드 및 결과 처리
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            is EditPostViewModel.UpdateStatus.Success -> {
                Toast.makeText(context, "게시물이 수정되었습니다!", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()
                navController.navigate("detail/$postId") {
                    popUpTo("edit/$postId") { inclusive = true }
                }
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
                existingImages = existingImages, // 서버에서 온 이미지 리스트 전달
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onSwapImages = viewModel::swapImages,
                onPreviewClick = { day, images ->
                    val locations = images.mapNotNull {
                        if (it.latitude != null && it.longitude != null) it.latitude to it.longitude else null
                    }
                    if (locations.isNotEmpty()) {
                        mapDialogLocations = locations
                        mapDialogTitle = "Day $day 위치 미리보기"
                        showMapDialog = true
                        viewModel.fetchRoute(locations)
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
                toFullUrl = { url -> toFullUrl(url) ?: "" }
            )
        }

        // --- 다이얼로그 모음 ---

        if (showCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                title = { Text("여행 유형 선택") },
                confirmButton = { TextButton(onClick = { viewModel.updateCategory("국내여행"); showCategoryDialog = false }) { Text("국내여행") } },
                dismissButton = { TextButton(onClick = { viewModel.updateCategory("국외여행"); showCategoryDialog = false }) { Text("국외여행") } }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateDateRange(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                        showDatePicker = false
                    }) { Text("확인") }
                }
            ) {
                DateRangePicker(state = dateRangePickerState, modifier = Modifier.height(500.dp))
            }
        }

        if (showMapDialog && mapDialogLocations.isNotEmpty()) {
            Dialog(onDismissRequest = { showMapDialog = false }) {
                Card(modifier = Modifier.fillMaxWidth().height(450.dp), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mapDialogTitle, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showMapDialog = false }) { Icon(Icons.Default.Close, null) }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            val firstLoc = mapDialogLocations.first()
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition(LatLng(firstLoc.first, firstLoc.second), 13.0)
                            }
                            NaverMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState
                            ) {
                                mapDialogLocations.forEachIndexed { i, loc ->
                                    Marker(state = MarkerState(position = LatLng(loc.first, loc.second)), captionText = "사진 ${i + 1}")
                                }
                                val polyCoords = routePoints.map { LatLng(it.latitude, it.longitude) }
                                if (polyCoords.isNotEmpty()) AnimatedPolyline(coords = polyCoords)
                            }
                        }
                    }
                }
            }
        }
    }
}

// URL 헬퍼 함수
private fun resolveBaseUrlForDevice(): String {
    val isEmulator = (Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator"))
    val phoneBaseUrl = runCatching { BuildConfig::class.java.getField("PHONE_BASE_URL").get(null) as String }.getOrNull()
    val raw = if(isEmulator) BuildConfig.BASE_URL else phoneBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL
    return raw.trimEnd('/') + "/"
}

private fun toFullUrl(urlOrPath: String?): String? {
    if(urlOrPath.isNullOrBlank()) return null
    if(urlOrPath.startsWith("http")) return urlOrPath
    return resolveBaseUrlForDevice() + urlOrPath.trimStart('/')
}