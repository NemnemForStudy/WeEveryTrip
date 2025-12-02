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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
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

/**
 * 1. [Stateful] WriteScreen
 */
@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // ViewModel 상태 관찰
    val postCreationStatus by viewModel.postCreationStatus.collectAsStateWithLifecycle()
    val latitude by viewModel.latitude.collectAsStateWithLifecycle()
    val longitude by viewModel.longitude.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val tripDays by viewModel.tripDays.collectAsStateWithLifecycle()
    val groupedImages by viewModel.groupedImages.collectAsStateWithLifecycle()

    // 게시글 등록 결과 처리
    LaunchedEffect(postCreationStatus) {
        when (val status = postCreationStatus) {
            is WriteViewModel.PostCreationStatus.Success -> {
                Toast.makeText(context, "게시물이 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()
                navController.popBackStack()
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
        onCreatePost = viewModel::createPost,
        onResetStatus = viewModel::resetStatus,
        onSwapImages = viewModel::swapImages
    )
}

/**
 * 2. [Stateless] WriteScreenContent
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onSwapImages: (Int, Int, Int) -> Unit // Day, From, To
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 로컬 UI 상태
    var showDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("카테고리") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    // 현재 선택된 Day탭 (0이면 전체, 1이면 Day1)
    var selectedDayTab by remember { mutableIntStateOf(0) }

    // ViewModel에 있는 startDate, endDate를 초기값으로 넣어주자
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate,
        initialSelectedEndDateMillis = endDate,
        yearRange = 2000..2050
    )

    // 임시로 선택된 URI 저장할 변수 (권한 허용 후 처리를 위해)
    var tempSelectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 권한 요청 런처 (사용자가 허용/거부 눌렀을 때 실행됨)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(isGranted) {
            // 허용하면 선택한 사진들 처리 시작
            if(tempSelectedUris.isNotEmpty()) {
                onProcessImages(tempSelectedUris)
            }
        } else {
            // 거부하면 위치 정보 없이 토스트 메세지
            Toast.makeText(context, "위치 정보를 가져오려면 권한이 필요합니다!", Toast.LENGTH_LONG).show()
        }
    }

    // 갤러리 런처(사진 선택 후 권한 체크)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            tempSelectedUris = uris // 선택한 URI 임시 저장

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if(hasPermission) {
                    // 이미 권한이 있으면 바로 처리
                    onProcessImages(uris)
                } else {
                    // 권한 없으면 요청 팝업 띄우기
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
            } else {
                // 안드로이드 9이하는 그냥 처리
                onProcessImages(uris)
            }
        }
    }

    // 카테고리 선택 다이얼로그
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "여행 유형 선택") },
            text = { Text(text = "작성할 글의 여행 유형을 선택해주세요.") },
            dismissButton = {
                TextButton(onClick = { category = "국내여행"; showDialog = false }) { Text("국내여행") }
            },
            confirmButton = {
                TextButton(onClick = { category = "국외여행"; showDialog = false }) { Text("국외여행") }
            }
        )
    }

    // 날짜 선택 다이얼로그 (수정됨)
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateDateRange(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                        showDatePickerDialog = false
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("취소") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "여행 기간 선택",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                },
                // 🔥 [핵심 수정] headline을 직접 정의해서 글자 깨짐 방지
                headline = {
                    val startDate = dateRangePickerState.selectedStartDateMillis
                    val endDate = dateRangePickerState.selectedEndDateMillis
                    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

                    val dateText = if (startDate != null && endDate != null) {
                        "${sdf.format(Date(startDate))} ~ ${sdf.format(Date(endDate))}"
                    } else if (startDate != null) {
                        "${sdf.format(Date(startDate))} ~ 선택 중"
                    } else {
                        "시작일 ~ 종료일"
                    }

                    Text(
                        text = dateText,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.headlineSmall, // 글자 크기 적절히 조절
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.fillMaxWidth().height(500.dp), // 높이 제한
                showModeToggle = true // 연필 아이콘(직접 입력 모드) 숨김 (필요하면 true)
            )
        }
    }

    // Drawer (오른쪽 슬라이드)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                        Spacer(Modifier.height(12.dp))
                        Text("메뉴", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider()

                        if(tripDays.isNotEmpty()) {
                            // 드롭다운 리스트 구현
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(tripDays.size) { index ->
                                    val dayNumber = index + 1
                                    val dayMillis = tripDays[index]
                                    val dayImages = groupedImages[dayNumber] ?: emptyList()

                                    // 각 Day별 확장 상태 관리
                                    // 초기값은 false임. var은 상태 바껴야 해서.
                                    var isExpanded by remember { mutableStateOf(false) }
                                    // var은 값을 바꿀 수 있어야 하는데 애니메이션 상탠 바꿀 수 없다.
                                    // 왜 val? -> isExpanded가 바뀌면 Compose가 알아서 0f에서 180로 부드럽게 숫자를 계산해주는 결과임.
                                    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow")
                                    Column {
                                        // Day 헤더 (클릭 시 확장/축소)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                // 클릭할 때마다 isExpanded 값을 true, false로 바꿈.
                                                .clickable { isExpanded = !isExpanded }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val sdf = SimpleDateFormat("MM.dd (E)", Locale.KOREA)
                                            Column {
                                                Text("Day $dayNumber", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text(sdf.format(Date(dayMillis)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("${dayImages.size}장", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "Drop Down",
                                                    modifier = Modifier.rotate(rotationState)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                                        // 확장된 이미지 리스트(순서 변경 가능)
                                        AnimatedVisibility(visible = isExpanded) {
                                            Column {
                                                dayImages.forEachIndexed { imgIndex, image ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // 썸네일
                                                        Image(
                                                            painter = rememberAsyncImagePainter(image.uri),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(60.dp)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))

                                                        // 시간 정보
                                                        val timeSdf = SimpleDateFormat("a hh:mm", Locale.KOREA)
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(timeSdf.format(Date(image.timestamp)), style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                        // 순서 변경 버튼(드래그 대신 클릭 - 안정성 확보)
                                                        Column {
                                                            if(imgIndex > 0) { // 위로 이동
                                                                Icon(
                                                                    Icons.Default.KeyboardArrowUp,
                                                                    contentDescription = "Up",
                                                                    modifier = Modifier.clickable { onSwapImages(dayNumber, imgIndex, imgIndex - 1) }
                                                                )
                                                            }
                                                            if(imgIndex < dayImages.size - 1) { // 아래로 이동
                                                                Icon(
                                                                    Icons.Default.KeyboardArrowDown,
                                                                    contentDescription = "Down",
                                                                    modifier = Modifier.clickable { onSwapImages(dayNumber, imgIndex, imgIndex + 1) }
                                                                )
                                                            }
                                                        }
                                                        // 드래그 핸들 아이콘(시각적 표시)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(Icons.Default.DragHandle, contentDescription = "Drag", tint = Color.Gray)
                                                    }

                                                    if(imgIndex < dayImages.size - 1) Divider(modifier = Modifier.padding(start = 88.dp))
                                                }
                                                if(dayImages.isEmpty()) {
                                                    Text("사진 없음", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                                }
                            },
                            actions = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "메뉴")
                                }
                                TextButton(onClick = {
                                    if (title.isNotEmpty() && content.isNotEmpty() && category != "카테고리") {
                                        val tagsList = tagsInput.split(" ", ",", "#").map { it.trim() }.filter { it.isNotEmpty() }
                                        // PostImage 객체 리스트를 Uri 리스트로 변환
                                        val allImages = groupedImages.values.flatten().map { it.uri }
                                        onCreatePost(category, title, content, tagsList, allImages)
                                    } else {
                                        Toast.makeText(context, "카테고리, 제목, 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("등록", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Beige)
                        )
                    },
                    containerColor = Beige
                    // innerPadding : Compose에서 부모가 준 여백 값을 의미 함.
                    // 이거 안쓰면 TopAppBar 아래에 그려야 할 콘텐츠가 AppBar 뒤에 가려짐.
                    // BottomBar에 내용이 막힘.
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // [카테고리 & 제목]
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clickable { showDialog = true }
                            ) {
                                Text(
                                    text = category,
                                    fontWeight = FontWeight.Bold,
                                    color = if (category == "카테고리") Color.Gray else Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("글 제목") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.LightGray,
                                    unfocusedIndicatorColor = Color.LightGray
                                ),
                                singleLine = true
                            )
                        }

                        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        // [날짜 선택 UI]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePickerDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))

                            val dateText = if (startDate != null && endDate != null) {
                                val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                                "${sdf.format(Date(startDate))} ~ ${sdf.format(Date(endDate))}"
                            } else "여행 기간을 선택해주세요"

                            Text(text = dateText, color = if (startDate != null) Color.Black else Color.Gray)
                        }

                        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                        // [사진 첨부 버튼]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { galleryLauncher.launch("image/*") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))

                            val totalCount = groupedImages.values.flatten().size
                            Text(text = "사진 첨부하기 ($totalCount)", color = Color.Gray)
                        }

                        // [이미지 리스트] (Day 필터링 적용)
                        val imagesToShow = remember(groupedImages, selectedDayTab) {
                            if (selectedDayTab == 0) groupedImages.values.flatten()
                            else groupedImages[selectedDayTab] ?: emptyList()
                        }

                        if (selectedDayTab != 0 && imagesToShow.isNotEmpty()) {
                            Text(
                                "Day $selectedDayTab 사진만 보는 중",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (imagesToShow.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(imagesToShow) { uri ->
                                    Box(modifier = Modifier.size(100.dp)) {
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        // 삭제 버튼 등 추가 가능
                                    }
                                }
                            }
                        }

                        // [지도 미리보기]
                        if (latitude != null && longitude != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("위치 정보 감지됨", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onUpdateLocation(null, null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "삭제", tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { navController.navigate("map?lat=$latitude&lon=$longitude") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("📍 지도에서 위치 미리보기") }
                        }

                        // [나머지 입력 필드들] - 이전에 괄호가 잘못 닫혀서 에러났던 부분 해결됨
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        TextField(
                            value = tagsInput, onValueChange = { tagsInput = it },
                            placeholder = { Text("#태그 입력") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        Divider()

                        TextField(
                            value = content, onValueChange = { content = it },
                            placeholder = { Text("내용을 입력하세요...") },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )

                        Spacer(modifier = Modifier.height(50.dp))
                    } // Column 닫기
                } // Scaffold 닫기
            }
        }
    }
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
            onSwapImages = { _, _, _ -> } // Day, From, To
        )
    }
}