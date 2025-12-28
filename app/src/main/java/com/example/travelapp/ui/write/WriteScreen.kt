package com.example.travelapp.ui.write

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.ui.components.PostForm
import com.example.travelapp.util.AnimatedPolyline
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalNaverMapApi::class)
@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 1. ViewModel 상태 관찰
    val postCreationStatus by viewModel.postCreationStatus.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val tripDays by viewModel.tripDays.collectAsStateWithLifecycle()
    val groupedImages by viewModel.groupedImages.collectAsStateWithLifecycle()
    val routePoints by viewModel.routePoints.collectAsStateWithLifecycle()

    // 2. 로컬 입력 상태
    var category by remember { mutableStateOf("국내여행") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    // 3. 다이얼로그 제어 상태
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var mapDialogLocations by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var mapDialogTitle by remember { mutableStateOf("") }

    // 날짜 선택 상태 관리
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate,
        initialSelectedEndDateMillis = endDate
    )

    // 4. 갤러리 및 권한 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.processSelectedImages(context, uris)
    }

    // 5. 게시글 등록 결과 처리
    LaunchedEffect(postCreationStatus) {
        when (postCreationStatus) {
            is WriteViewModel.PostCreationStatus.Success -> {
                val postId = (postCreationStatus as WriteViewModel.PostCreationStatus.Success).postId
                Toast.makeText(context, "게시물이 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()
                navController.navigate("detail/$postId") {
                    // write라는 경로를 찾아서 그 위까지 다 지움.
                    // inclusive = true를 주면 write 페이지 자체도 스택에서 지운다.
                    popUpTo("write") { inclusive = true }
                }
            }
            is WriteViewModel.PostCreationStatus.Error -> {
                val message = (postCreationStatus as WriteViewModel.PostCreationStatus.Error).message
                Toast.makeText(context, "오류: $message", Toast.LENGTH_LONG).show()
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    // --- UI 그리기 ---
    Box(modifier = Modifier.fillMaxSize()) {
        PostForm(
            navController = navController,
            drawerState = drawerState,
            title = title,
            onTitleChange = { title = it },
            content = content,
            onContentChange = { content = it },
            category = category,
            onCategoryClick = { showCategoryDialog = true },
            tagsInput = tagsInput,
            onTagsChange = { tagsInput = it },
            startDate = startDate,
            endDate = endDate,
            onDateClick = { showDatePicker = true },
            tripDays = tripDays,
            groupedImages = groupedImages,
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
            submitButtonText = "게시",
            isSubmitting = postCreationStatus is WriteViewModel.PostCreationStatus.Loading,
            onSubmitClick = {
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    val tags = tagsInput.split(" ", ",", "#").filter { it.isNotBlank() }
                    val allImages = groupedImages.values.flatten().map { it.uri }
                    viewModel.createPost(category, title, content, tags, allImages)
                } else {
                    Toast.makeText(context, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // --- 다이얼로그 모음 ---

        // 1. 카테고리 선택 다이얼로그
        if (showCategoryDialog) {
            ModalBottomSheet(
                onDismissRequest = { showCategoryDialog = false },
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("어떤 여행인가요?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(24.dp))

                    // 버튼을 꽉 차게 만들고 디자인 입히기
                    CategoryItem("국내여행", isSelected = category == "국내여행") {
                        category = "국내여행"; showCategoryDialog = false
                    }
                    Spacer(Modifier.height(12.dp))
                    CategoryItem("국외여행", isSelected = category == "국외여행") {
                        category = "국외여행"; showCategoryDialog = false
                    }
                }
            }
        }

        // 2. 날짜 선택 다이얼로그
        if (showDatePicker) {
            MaterialTheme(colorScheme = lightColorScheme(
                surface = Color.White,
                onSurface = Color.Black,
                primary = Color(0xFF1976D2),
                onPrimary = Color.White
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
                            Text("확인", fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("취소", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                ) {
                    val sdf = SimpleDateFormat("yy년 MM월 dd일", Locale.KOREA)
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.height(500.dp),
                        title = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "여행 기간 선택",
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        headline = {
                            val start = dateRangePickerState.selectedStartDateMillis
                            val end = dateRangePickerState.selectedEndDateMillis
                            val text = if(start != null && end != null) {
                                "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
                            } else {
                                "시작일 - 종료일"
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center // 텍스트를 박스 정중앙에 배치
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 18.sp,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center // 텍스트 자체도 중앙 정렬
                                )
                            }
                        },
                    )
                }
            }
        }

        // 3. 지도 미리보기 팝업
        if (showMapDialog && mapDialogLocations.isNotEmpty()) {
            Dialog(onDismissRequest = { showMapDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(500.dp),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                mapDialogTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            Surface(
                                onClick = { showMapDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.align(Alignment.CenterEnd).size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, "닫기", modifier = Modifier.padding(8.dp))
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f)
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                        ) {
                            val firstLoc = mapDialogLocations.first()
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition(LatLng(firstLoc.first, firstLoc.second), 13.0)
                            }

                            NaverMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(isZoomControlEnabled = true)
                            ) {
                                mapDialogLocations.forEachIndexed { index, loc ->
                                    Marker(
                                        state = MarkerState(
                                            position = LatLng(
                                                loc.first,
                                                loc.second
                                            )
                                        ),
                                        captionText = "사진 ${index + 1}"
                                    )
                                }

                                val polylineCoords = remember<List<LatLng>>(routePoints) { // 1. remember가 리스트를 감시하도록 설정
                                    val pointsList: List<RoutePoint> = routePoints
                                    pointsList.map { point ->
                                        LatLng(point.latitude, point.longitude)
                                    }
                                }

                                if (polylineCoords.isNotEmpty()) {
                                    AnimatedPolyline(coords = polylineCoords)
                                }
                            }
                        }
                    }
                }
            }
        }

        if(postCreationStatus is WriteViewModel.PostCreationStatus.Loading) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 4.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "소중한 추억을 서버에 올리는 중...",
                        color = Color.White,
                        fontWeight = FontWeight.Black, // 우리 컨셉에 맞게 더 두껍게
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

    }
}

@Composable
fun CategoryItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF1976D2).copy(alpha = 0.1f) else Color(0xFFF5F5F5),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF1976D2)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium)
        }
    }
}