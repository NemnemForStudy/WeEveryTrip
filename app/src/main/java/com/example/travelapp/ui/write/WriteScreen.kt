package com.example.travelapp.ui.write

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.ui.theme.Beige
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.util.AnimatedPolyline
import com.example.travelapp.util.ExifUtils
import com.example.travelapp.util.ExifUtils.extractLocation
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.naver.maps.map.compose.*
import kotlinx.coroutines.invoke
import kotlin.math.roundToInt

private const val MAX_PHOTOS = 15
/**
 * 1. [Stateful] WriteScreen
 */
@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel 상태 관찰
    val postCreationStatus by viewModel.postCreationStatus.collectAsStateWithLifecycle()
    val latitude by viewModel.latitude.collectAsStateWithLifecycle()
    val longitude by viewModel.longitude.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val tripDays by viewModel.tripDays.collectAsStateWithLifecycle()
    val groupedImages by viewModel.groupedImages.collectAsStateWithLifecycle()
    val routePoints by viewModel.routePoints.collectAsStateWithLifecycle()

    // 게시글 등록 결과 처리
    LaunchedEffect(postCreationStatus) {
        when (val status = postCreationStatus) {
            is WriteViewModel.PostCreationStatus.Success -> {
                Toast.makeText(context, "게시물이 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()
                navController.navigate("detail/${status.postId}")
            }
            is WriteViewModel.PostCreationStatus.Error -> {
                Toast.makeText(context, "오류: ${status.message}", Toast.LENGTH_LONG).show()
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    // UI 그리기
    WriteScreenContent(
        navController = navController,
        postCreationStatus = postCreationStatus,
        latitude = latitude,
        longitude = longitude,
        startDate = startDate,
        endDate = endDate,
        tripDays = tripDays,
        groupedImages = groupedImages, // ViewModel 데이터 전달
        onUpdateLocation = viewModel::updateLocation,
        onUpdateDateRange = viewModel::updateDateRange,
        onProcessImages = { uris -> viewModel.processSelectedImages(context, uris) },
        onCreatePost = { category, title, content, tags, images ->
            viewModel.createPost(category, title, content, tags, images)
       },
        onResetStatus = viewModel::resetStatus,
        onSwapImages = viewModel::swapImages,
        onFetchRoute = viewModel::fetchRoute,
        routePoints = routePoints
    )
}

/**
 * 2. [Stateless] WriteScreenContent
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalNaverMapApi::class)
@Composable
fun WriteScreenContent(
    navController: NavController,
    postCreationStatus: WriteViewModel.PostCreationStatus,
    latitude: Double?,
    longitude: Double?,
    startDate: Long?,
    endDate: Long?,
    tripDays: List<Long>,
    groupedImages: Map<Int, List<PostImage>>,
    onUpdateLocation: (Double?, Double?) -> Unit,
    onUpdateDateRange: (Long?, Long?) -> Unit,
    onProcessImages: (List<Uri>) -> Unit,
    onCreatePost: (String, String, String, List<String>, List<Uri>) -> Unit,
    onResetStatus: () -> Unit,
    onSwapImages: (Int, Int, Int) -> Unit, // Day, From, To
    onFetchRoute: (List<Pair<Double, Double>>) -> Unit,
    routePoints: List<RoutePoint>
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 로컬 UI 상태
    var showDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // GPS 없는 사진 경고 다이얼로그 열지 여부
    var showNoGpsDialog by remember { mutableStateOf(false) }

    // 게시 버튼 눌렀을 때 실제 업로드를 실행할 지 저장해두는 플래그
    var pendingSubmit by remember { mutableStateOf(false) }

    // GPS 없는 사진 개수
    val noGpsCount = remember(groupedImages) {
        groupedImages.values
            .flatten()
            .count { it.latitude == null || it.longitude == null }
    }

    // 전체 사진 수
    val totalPhotoCount = remember(groupedImages) {
        groupedImages.values.flatten().size
    }

    var category by remember { mutableStateOf("카테고리") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    // 현재 선택된 Day탭 (0이면 전체, 1이면 Day1)
    var selectedDayTab by remember { mutableIntStateOf(0) }

    // 🔥 [지도 팝업 상태] WriteScreenContent 내부로 이동
    var showMapDialog by remember { mutableStateOf(false) }
    var mapDialogLocations by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var mapDialogTitle by remember { mutableStateOf("") }

    // ViewModel에 있는 startDate, endDate를 초기값으로 넣어주자
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate,
        initialSelectedEndDateMillis = endDate,
        yearRange = 2000..2050
    )

    // 임시로 선택된 URI 저장할 변수 (권한 허용 후 처리를 위해)
    var tempSelectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 권한 요청 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (tempSelectedUris.isNotEmpty()) {
                onProcessImages(tempSelectedUris)
            }
        } else {
            Toast.makeText(context, "위치 정보를 가져오려면 권한이 필요합니다!", Toast.LENGTH_LONG).show()
        }
    }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if(uris.size > MAX_PHOTOS) {
                Toast.makeText(context, "최대 ${MAX_PHOTOS}장까지만 첨부할 수 있습니다.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            tempSelectedUris = uris
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    onProcessImages(uris)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
            } else {
                onProcessImages(uris)
            }
        }
    }

    // 카테고리 다이얼로그
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "여행 유형 선택") },
            text = { Text(text = "작성할 글의 여행 유형을 선택해주세요.") },
            dismissButton = { TextButton(onClick = { category = "국내여행"; showDialog = false }) { Text("국내여행") } },
            confirmButton = { TextButton(onClick = { category = "국외여행"; showDialog = false }) { Text("국외여행") } }
        )
    }

    // 날짜 선택 다이얼로그
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateDateRange(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis)
                    showDatePickerDialog = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerDialog = false }) { Text("취소") } }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("여행 기간 선택", modifier = Modifier.padding(start = 24.dp, top = 16.dp), fontWeight = FontWeight.Bold) },
                headline = {
                    val s = dateRangePickerState.selectedStartDateMillis
                    val e = dateRangePickerState.selectedEndDateMillis
                    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                    val txt = if (s != null && e != null) "${sdf.format(Date(s))} ~ ${sdf.format(Date(e))}" else if (s != null) "${sdf.format(Date(s))} ~ 선택 중" else "시작일 ~ 종료일"
                    Text(txt, modifier = Modifier.padding(start = 24.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                showModeToggle = true
            )
        }
    }

    // 🔥 [1] 최상위를 Box로 감쌉니다. (팝업 오버레이를 위해)
    Box(modifier = Modifier.fillMaxSize()) {

        // [2] 기존 메인 UI (Drawer + Scaffold)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                            Spacer(Modifier.height(12.dp))

                            // Drawer 헤더 (좌: 제목, 우: 게시 버튼)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("여행 일정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                TextButton(onClick = {
                                    if (title.isNotEmpty() && content.isNotEmpty() && category != "카테고리") {
                                        // GPS 없는 사진 있으면 -> 경고, 다이얼로그 띄우고 종료
                                        if(noGpsCount > 0) {
                                            showNoGpsDialog = true
                                            pendingSubmit = true
                                            return@TextButton
                                        }

                                        // GPS 문제 없으면 바로 업로드
                                        val tagsList = tagsInput.split(" ", ",", "#")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                        val allImages = groupedImages.values.flatten().map { it.uri }
                                        onCreatePost(category, title, content, tagsList, allImages)
                                    } else {
                                        Toast.makeText(context, "카테고리, 제목, 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("게시", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            HorizontalDivider()

                            // 일정 리스트
                            if (tripDays.isNotEmpty()) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(tripDays.size) { index ->
                                        val dayNumber = index + 1
                                        val dayMillis = tripDays[index]
                                        val dayImages = groupedImages[dayNumber] ?: emptyList()
                                        var isExpanded by remember { mutableStateOf(false) }
                                        val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow")

                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isExpanded = !isExpanded }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val sdf = SimpleDateFormat("MM.dd (E)", Locale.KOREA)
                                                Column {
                                                    Text("Day $dayNumber", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text(sdf.format(Date(dayMillis)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }

                                                // 우측: 미리보기 버튼 + 장수 + 아이콘
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (dayImages.isNotEmpty()) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                // 비동기로 위치 추출 후 팝업 열기
                                                                scope.launch {
                                                                    val extractedLocations = withContext(Dispatchers.IO) {
                                                                        dayImages.mapNotNull { ExifUtils.extractLocation(context, it.uri) }
                                                                    }

                                                                        if(extractedLocations.isNotEmpty()) {
                                                                            mapDialogLocations = extractedLocations
                                                                            mapDialogTitle = "Day $dayNumber 위치 미리보기"
                                                                            showMapDialog = true

                                                                            onFetchRoute(extractedLocations)
                                                                        } else {
                                                                            Toast.makeText(context, "이 사진에는 위치 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                }
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                            modifier = Modifier
                                                                .height(32.dp)
                                                                .padding(end = 8.dp),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text("미리보기", fontSize = 12.sp)
                                                        }
                                                    }
                                                    Text("${dayImages.size}장", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    Icon(Icons.Default.ArrowDropDown, "Drop Down", modifier = Modifier.rotate(rotationState))
                                                }
                                            }
                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                                            // 확장된 이미지 리스트
                                            AnimatedVisibility(visible = isExpanded) {
                                                val density = LocalDensity.current
                                                val itemHeightDp = 76.dp
                                                val itemHeightPx = with(density) { itemHeightDp.toPx() }

                                                var draggingIndex by remember(dayNumber) { mutableIntStateOf(-1) }
                                                var dragOffsetY by remember(dayNumber) { mutableFloatStateOf(0f)}
                                                Column {
                                                    dayImages.forEachIndexed { imgIndex, image ->
                                                        val isDragging = imgIndex == draggingIndex

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    horizontal = 16.dp,
                                                                    vertical = 8.dp
                                                                )
                                                                .zIndex(if(isDragging) 1f else 0f)
                                                                .offset {
                                                                    if(isDragging) IntOffset(0, dragOffsetY.roundToInt()) else IntOffset.Zero
                                                                },
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Image(
                                                                painter = rememberAsyncImagePainter(
                                                                    image.uri
                                                                ),
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(60.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                            Spacer(Modifier.width(12.dp))
                                                            val timeSdf = SimpleDateFormat(
                                                                "a hh:mm",
                                                                Locale.KOREA
                                                            )
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    timeSdf.format(Date(image.timestamp)),
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                            }

                                                            Spacer(Modifier.width(8.dp))

                                                            Icon(
                                                                imageVector = Icons.Default.DragHandle,
                                                                contentDescription = "Drag",
                                                                tint = Color.Gray,
                                                                modifier = Modifier.pointerInput(
                                                                    dayNumber,
                                                                    dayImages.size,
                                                                    imgIndex
                                                                ) {
                                                                    detectDragGesturesAfterLongPress(
                                                                        onDragStart = {
                                                                            draggingIndex = imgIndex
                                                                            dragOffsetY = 0f
                                                                        },
                                                                        onDrag = { change, dragAmount ->
                                                                            change.consume()
                                                                            if (draggingIndex == -1) return@detectDragGesturesAfterLongPress

                                                                            dragOffsetY += dragAmount.y

                                                                            val deltaIndex =
                                                                                (dragOffsetY / itemHeightPx).toInt()
                                                                            val targetIndex =
                                                                                (draggingIndex + deltaIndex)
                                                                                    .coerceIn(
                                                                                        0,
                                                                                        dayImages.lastIndex
                                                                                    )

                                                                            if (targetIndex != draggingIndex) {
                                                                                val from =
                                                                                    draggingIndex
                                                                                onSwapImages(
                                                                                    dayNumber,
                                                                                    from,
                                                                                    targetIndex
                                                                                )
                                                                                draggingIndex =
                                                                                    targetIndex
                                                                                dragOffsetY -= (targetIndex - from) * itemHeightPx
                                                                            }
                                                                        },
                                                                        onDragEnd = {
                                                                            draggingIndex = -1
                                                                            dragOffsetY = 0f
                                                                        },
                                                                        onDragCancel = {
                                                                            draggingIndex = -1
                                                                            dragOffsetY = 0f
                                                                        }
                                                                    )
                                                                }
                                                            )
                                                        }
                                                        if (imgIndex < dayImages.size - 1) {
                                                            Divider(modifier = Modifier.padding(start = 88.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("날짜를 먼저 선택해주세요", modifier = Modifier.padding(16.dp), color = Color.Gray)
                            }
                        }
                    }
                }
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("글쓰기", fontWeight = FontWeight.Bold) },
                                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "뒤로가기") } },
                                actions = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "메뉴") }
                                    TextButton(onClick = {
                                        if (title.isNotEmpty() && content.isNotEmpty() && category != "카테고리") {
                                            if (noGpsCount > 0) {
                                                showNoGpsDialog = true
                                                pendingSubmit = true
                                                return@TextButton
                                            }

                                            val tagsList = tagsInput.split(" ", ",", "#")
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }
                                            val allImages = groupedImages.values.flatten().map { it.uri }
                                            onCreatePost(category, title, content, tagsList, allImages)
                                        } else {
                                            Toast.makeText(context, "카테고리, 제목, 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Text("게시", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Beige)
                            )
                        },
                        containerColor = Beige
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 카테고리 & 제목
                            Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clickable { showDialog = true }) {
                                    Text(category, fontWeight = FontWeight.Bold, color = if (category == "카테고리") Color.Gray else Color.Black)
                                }
                                Spacer(Modifier.width(8.dp))
                                TextField(value = title, onValueChange = { title = it }, placeholder = { Text("글 제목") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), singleLine = true)
                            }
                            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                            // 날짜 선택
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePickerDialog = true }
                                .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, null, tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                val dateText = if (startDate != null && endDate != null) "${SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(startDate))} ~ ${SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(endDate))}" else "여행 기간을 선택해주세요"
                                Text(dateText, color = if (startDate != null) Color.Black else Color.Gray)
                            }
                            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                            // 사진 첨부
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { galleryLauncher.launch("image/*") }
                                .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                val totalCount = groupedImages.values.flatten().size
                                Text("사진 첨부하기 ($totalCount)", color = Color.Gray)
                            }

                            // 사진 리스트
                            val imagesToShow = remember(groupedImages, selectedDayTab) {
                                if (selectedDayTab == 0) groupedImages.values.flatten() else groupedImages[selectedDayTab] ?: emptyList()
                            }
                            if (selectedDayTab != 0 && imagesToShow.isNotEmpty()) Text("Day $selectedDayTab 사진만 보는 중", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                            if (imagesToShow.isNotEmpty()) {
                                LazyRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(imagesToShow) { image ->
                                        Box(modifier = Modifier.size(100.dp)) {
                                            Image(painter = rememberAsyncImagePainter(image.uri), contentDescription = null, modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                        }
                                    }
                                }
                            }

                            TextField(value = tagsInput, onValueChange = { tagsInput = it }, placeholder = { Text("#태그 입력") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                            Divider()
                            TextField(value = content, onValueChange = { content = it }, placeholder = { Text("내용을 입력하세요...") }, modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                            Spacer(Modifier.height(50.dp))
                        }
                    }
                }
            }
        }

        // 🔥 [3] 지도 팝업 (Box 오버레이 방식)
        if (showMapDialog && mapDialogLocations.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(10f)
                    .clickable { showMapDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(400.dp)
                        .zIndex(11f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    onClick = {  }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = mapDialogTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { showMapDialog = false }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "닫기") }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            val firstLocation = mapDialogLocations.first() // 첫번째 위치 중심 좌표 사용.
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition(
                                    LatLng(firstLocation.first,firstLocation.second),
                                    14.0
                                )
                            }
                            NaverMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(isZoomControlEnabled = true, isScrollGesturesEnabled = true, isZoomGesturesEnabled = true, isLogoClickEnabled = false)
                            ) {
                                mapDialogLocations.forEachIndexed { index, locationPair ->
                                    Marker(
                                        state = MarkerState(position = LatLng(locationPair.first, locationPair.second)),
                                        captionText = "사진 ${index + 1}" // 마커마다 번호 부여
                                    )
                                }

                                val polylineCoords  = remember(routePoints) {
                                    routePoints.map { LatLng(it.latitude, it.longitude) }
                                }

                                AnimatedPolyline(coords = polylineCoords)
                            }
                        }
                    }
                }
            }
        }

        if(showNoGpsDialog) {
            AlertDialog(
                onDismissRequest = {
                    showNoGpsDialog = false
                    pendingSubmit = false
                },
                title = { Text("위치 정보(GPS)가 없는 사진이 있어요") },
                text = {
                    Text(
                        "총 ${totalPhotoCount}장 중 ${noGpsCount}장에 위치 정보가 없습니다.\n" +
                                "이 사진들은 상세 지도에서 마커/경로에 포함되지 않을 수 있어요.\n" +
                                "그래도 게시할까요?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showNoGpsDialog = false

                        if(pendingSubmit) {
                            pendingSubmit = false

                            val tagsList = tagsInput.split(" ", ",", "#")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            val allImages = groupedImages.values.flatten().map { it.uri }
                            onCreatePost(category, title, content, tagsList, allImages)
                        }
                    }) { Text("계속 게시") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showNoGpsDialog = false
                        pendingSubmit = false
                    }) { Text("취소") }
                }
            )
        }
    } // 최상위 Box 닫기
}

/**
 * 3. [Preview]
 */
@Preview(showBackground = true)
@Composable
fun WriteScreenPreview() {
    TravelAppTheme {
        WriteScreenContent(
            navController = rememberNavController(),
            postCreationStatus = WriteViewModel.PostCreationStatus.Idle,
            latitude = 37.5665,
            longitude = 126.9779,
            startDate = null,
            endDate = null,
            tripDays = listOf(1733065200000, 1733151600000), // 프리뷰용 더미 날짜
            groupedImages = emptyMap(), // 🔥 [중요] Preview에 파라미터 추가!
            onUpdateLocation = { _, _ -> },
            onUpdateDateRange = { _, _ -> },
            onProcessImages = {},
            onCreatePost = { _, _, _, _, _ -> },
            onResetStatus = {},
            onSwapImages = { _, _, _ -> }, // Day, From, To
            onFetchRoute = {},
            routePoints = emptyList()
        )
    }
}