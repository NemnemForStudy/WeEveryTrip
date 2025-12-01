package com.example.travelapp.ui.write

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.travelapp.util.ExifUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1. [Stateful] WriteScreen
 */
@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val postCreationStatus by viewModel.postCreationStatus.collectAsStateWithLifecycle()
    val latitude by viewModel.latitude.collectAsStateWithLifecycle()
    val longitude by viewModel.longitude.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val tripDays by viewModel.tripDays.collectAsStateWithLifecycle()

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
        onUpdateLocation = viewModel::updateLocation,
        onUpdateDateRange = viewModel::updateDateRange,
        onProcessImages = { uris -> viewModel.processSeletedImages(context, uris) },
        onCreatePost = viewModel::createPost,
        onResetStatus = viewModel::resetStatus
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
    onUpdateLocation: (Double?, Double?) -> Unit,
    onUpdateDateRange: (Long?, Long?) -> Unit,
    onProcessImages: (List<Uri>) -> Unit,
    onCreatePost: (String, String, String, List<String>, List<Uri>) -> Unit,
    onResetStatus: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 로컬 UI 상태
    var showDialog by remember { mutableStateOf(true) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("카테고리") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImageUris = uris
        if (uris.isNotEmpty()) {
            Toast.makeText(context, "${uris.size}개의 사진이 선택되었습니다.", Toast.LENGTH_SHORT).show()

            onProcessImages(uris)

            val firstLocation = uris.asSequence()
                .mapNotNull { ExifUtils.extractLocation(context, it) }
                .firstOrNull()

            if (firstLocation != null) {
                onUpdateLocation(firstLocation.first, firstLocation.second)
                Toast.makeText(context, "사진 위치 정보를 불러왔습니다!", Toast.LENGTH_SHORT).show()
            } else {
                onUpdateLocation(null, null)
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

    // ⭐️ [수정됨] 날짜 선택 다이얼로그
    if (showDatePickerDialog) {
        val dateRangePickerState = rememberDateRangePickerState()
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
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                },
                headline = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val startMills = dateRangePickerState.selectedStartDateMillis
                        val endMills = dateRangePickerState.selectedEndDateMillis

                        val headlineText = if(startMills != null && endMills != null) {
                            val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                            "${sdf.format(Date(startMills))} - ${sdf.format(Date(endMills))}"
                        } else if(startMills != null) {
                            val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                            "${sdf.format(Date(startMills))} - 종료일"
                        } else {
                            "시작일 - 종료일"
                        }
                        Text(
                            text = headlineText,
                            style = MaterialTheme.typography.headlineMedium,


                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(500.dp),
                showModeToggle = false
            )
        }
    }

    // Drawer (오른쪽 슬라이드)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                        Spacer(Modifier.height(12.dp))
                        Text("메뉴", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider()

                        if(tripDays.isNotEmpty()) {
                            Text(
                                "여행 일정 (${tripDays.size}일",
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val sdf = SimpleDateFormat("MM/dd (E)", Locale.KOREA)

                            tripDays.forEachIndexed { index, dayMillis ->
                                NavigationDrawerItem(
                                    label = { Text("Day ${index + 1}: ${sdf.format(Date(dayMillis))}") },
                                    selected = false,
                                    onClick = {
                                        // TODO: 해당 날짜의 사진만 필터링해서 보여주는 기능 연결
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        NavigationDrawerItem(
                            label = { Text("임시 저장 목록") },
                            selected = false,
                            onClick = { /* TODO */ }
                        )
                        NavigationDrawerItem(
                            label = { Text("설정") },
                            selected = false,
                            onClick = { /* TODO */ }
                        )
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
                                IconButton(onClick = {
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "메뉴")
                                }

                                TextButton(onClick = {
                                    if (title.isNotEmpty() && content.isNotEmpty() && category != "카테고리") {
                                        val tagsList = tagsInput.split(" ", ",", "#").map { it.trim() }.filter { it.isNotEmpty() }
                                        onCreatePost(category, title, content, tagsList, selectedImageUris)
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
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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

                        // ⭐️ [날짜 선택 UI]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePickerDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "날짜 선택", tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))

                            // ⭐️ [확인] 여기서 한국어 포맷(Locale.KOREA)을 사용하여 결과를 표시합니다.
                            val dateText = if (startDate != null && endDate != null) {
                                val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                                "${sdf.format(Date(startDate))} ~ ${sdf.format(Date(endDate))}"
                            } else {
                                "여행 기간을 선택해주세요"
                            }

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
                            Icon(Icons.Default.CameraAlt, contentDescription = "사진 첨부", tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "사진 첨부하기 (${selectedImageUris.size})", color = Color.Gray)
                        }

                        // [이미지 미리보기 리스트]
                        if (selectedImageUris.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedImageUris) { uri ->
                                    Box(modifier = Modifier.size(100.dp)) {
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { selectedImageUris = selectedImageUris - uri },
                                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.3f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // ⭐️ [지도 미리보기 & 삭제 버튼]
                        if (latitude != null && longitude != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "사진 위치 정보가 감지되었습니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                // 위치 삭제 버튼
                                IconButton(onClick = {
                                    Toast.makeText(context, "위치 정보가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                                    onUpdateLocation(null, null)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "삭제", tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { navController.navigate("map?lat=$latitude&lon=$longitude") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("📍 지도에서 위치 미리보기")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else if (selectedImageUris.isNotEmpty()) {
                            // 사진은 있지만 위치 정보가 없는 경우
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("선택한 사진에 위치 정보가 없습니다.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                        // [태그 입력]
                        TextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            placeholder = { Text("#태그 입력") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                        // [본문 입력]
                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = { Text("내용을 입력하세요...") },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
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
            onUpdateLocation = { _, _ -> },
            onUpdateDateRange = { _, _ -> },
            onProcessImages = {},
            onCreatePost = { _, _, _, _, _ -> },
            onResetStatus = {}
        )
    }
}