package com.example.travelapp.ui.edit

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.travelapp.BuildConfig
import com.example.travelapp.ui.theme.Beige
import com.example.travelapp.ui.write.PostImage
import com.example.travelapp.util.DateUtils
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val MAX_PHOTOS = 15

@OptIn(ExperimentalMaterial3Api::class, ExperimentalNaverMapApi::class)
@Composable
fun EditPostScreen(
    navController: NavController,
    postId: String,
    viewModel: EditPostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // ViewModel 상태
    val post by viewModel.post.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val latitude by viewModel.latitude.collectAsStateWithLifecycle()
    val longitude by viewModel.longitude.collectAsStateWithLifecycle()
    val isDomestic by viewModel.isDomestic.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val tripDays by viewModel.tripDays.collectAsStateWithLifecycle()
    val groupedImages by viewModel.groupedImages.collectAsStateWithLifecycle()

    // UI 상태
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var tagsInput by remember { mutableStateOf("") }
    var selectedDayTab by remember { mutableIntStateOf(0) }
    var showMapDialog by remember { mutableStateOf(false) }
    var mapDialogLocations by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var mapDialogTitle by remember { mutableStateOf("") }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate,
        initialSelectedEndDateMillis = endDate,
        yearRange = 2000..2050
    )

    var tempSelectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 게시물 로드
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    // 수정 결과 처리
    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            is EditPostViewModel.UpdateStatus.Success -> {
                Toast.makeText(context, "게시물이 수정되었습니다!", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()

                navController.navigate("detail/$postId") {
                    popUpTo("edit/$postId") { inclusive = true }
                    launchSingleTop = true
                }
            }
            is EditPostViewModel.UpdateStatus.Error -> {
                Toast.makeText(context, (updateStatus as EditPostViewModel.UpdateStatus.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    // 권한 요청 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (tempSelectedUris.isNotEmpty()) {
                // TODO: viewModel.processSelectedImages(context, tempSelectedUris)
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
            if (uris.size > MAX_PHOTOS) {
                Toast.makeText(context, "최대 ${MAX_PHOTOS}장까지만 첨부할 수 있습니다.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            tempSelectedUris = uris
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_MEDIA_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    viewModel.processSelectedImages(context, uris)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
            } else {
                viewModel.processSelectedImages(context, uris)
            }
        }
    }

    // 카테고리 다이얼로그
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("여행 유형 선택") },
            text = { Text("수정할 글의 여행 유형을 선택해주세요.") },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updateCategory("국내여행")
                    showCategoryDialog = false
                }) { Text("국내여행") }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateCategory("국외여행")
                    showCategoryDialog = false
                }) { Text("국외여행") }
            }
        )
    }

    // 날짜 선택 다이얼로그
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDateRange(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
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
                    val txt = if (s != null && e != null) "${sdf.format(Date(s))} ~ ${sdf.format(Date(e))}"
                    else if (s != null) "${sdf.format(Date(s))} ~ 선택 중"
                    else "시작일 ~ 종료일"
                    Text(txt, modifier = Modifier.padding(start = 24.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                },
                modifier = Modifier.fillMaxWidth().height(500.dp),
                showModeToggle = true
            )
        }
    }

    // 메인 UI
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("여행 일정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = {
                                        if (title.isNotEmpty() && content.isNotEmpty()) {
                                            viewModel.updatePost(postId, context)
                                        } else {
                                            Toast.makeText(context, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Text("수정", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                HorizontalDivider()

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
                                                    modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(horizontal = 16.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val sdf = SimpleDateFormat("MM.dd (E)", Locale.KOREA)
                                                    Column {
                                                        Text("Day $dayNumber", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Text(DateUtils.formatToDisplay(dayMillis), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        // 미리보기 버튼
                                                        if (dayImages.isNotEmpty()) {
                                                            Button(
                                                                onClick = {
                                                                    val extractedLocations = dayImages.mapNotNull { img ->
                                                                        if (img.latitude != null && img.longitude != null) {
                                                                            com.example.travelapp.data.model.RoutePoint(img.latitude, img.longitude)
                                                                        } else null
                                                                    }
                                                                    if (extractedLocations.isNotEmpty()) {
                                                                        Toast.makeText(context, "Day $dayNumber 위치: ${extractedLocations.size}개", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, "이 날의 사진에는 위치 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                                modifier = Modifier.height(32.dp).padding(end = 8.dp),
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

                                                AnimatedVisibility(visible = isExpanded) {
                                                    val density = LocalDensity.current
                                                    val itemHeightDp = 76.dp
                                                    val itemHeightPx = with(density) { itemHeightDp.toPx() }

                                                    var draggingIndex by remember(dayNumber) { mutableIntStateOf(-1) }
                                                    var dragOffsetY by remember(dayNumber) { mutableFloatStateOf(0f) }

                                                    Column {
                                                        dayImages.forEachIndexed { imgIndex, image ->
                                                            val isDragging = imgIndex == draggingIndex

                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                                                    .zIndex(if (isDragging) 1f else 0f)
                                                                    .offset { if (isDragging) IntOffset(0, dragOffsetY.roundToInt()) else IntOffset.Zero },
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Image(
                                                                    painter = rememberAsyncImagePainter(image.uri),
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                                Spacer(Modifier.width(12.dp))
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    // 시간 포맷팅
                                                                    val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREA)
                                                                    // image 객체 안 timestamp(Long)가 있다면 사용
                                                                    val timeString = if(image.timestamp != null) {
                                                                        println("DEBUG: ${image.uri} -> timestamp: ${image.timestamp}") // 이 로그 확인!
                                                                        timeFormat.format(Date(image.timestamp))
                                                                    } else {
                                                                        "${imgIndex + 1}번째 사진" // 백업용으로 순서 표시
                                                                    }
                                                                    Text(
                                                                        text = timeString,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                }
                                                                Spacer(Modifier.width(8.dp))
                                                                // 드래그 핸들 (꾹 누르면 드래그)
                                                                Icon(
                                                                    imageVector = Icons.Default.DragHandle,
                                                                    contentDescription = "Drag",
                                                                    tint = Color.Gray,
                                                                    modifier = Modifier.pointerInput(dayNumber, dayImages.size, imgIndex) {
                                                                        detectDragGesturesAfterLongPress(
                                                                            onDragStart = {
                                                                                draggingIndex = imgIndex
                                                                                dragOffsetY = 0f
                                                                            },
                                                                            onDrag = { change, dragAmount ->
                                                                                change.consume()
                                                                                if (draggingIndex == -1) return@detectDragGesturesAfterLongPress

                                                                                dragOffsetY += dragAmount.y

                                                                                val deltaIndex = (dragOffsetY / itemHeightPx).toInt()
                                                                                val targetIndex = (draggingIndex + deltaIndex).coerceIn(0, dayImages.lastIndex)

                                                                                if (targetIndex != draggingIndex) {
                                                                                    val from = draggingIndex
                                                                                    viewModel.swapImages(dayNumber, from, targetIndex)
                                                                                    draggingIndex = targetIndex
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
                                                            if (imgIndex < dayImages.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 88.dp))
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
                                    title = { Text("게시물 수정", fontWeight = FontWeight.Bold) },
                                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "뒤로가기") } },
                                    actions = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "메뉴") }
                                        TextButton(onClick = {
                                            if (title.isNotEmpty() && content.isNotEmpty()) {
                                                viewModel.updatePost(postId, context)
                                            } else {
                                                Toast.makeText(context, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            if (updateStatus is EditPostViewModel.UpdateStatus.Loading) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text("수정", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Beige)
                                )
                            },
                            containerColor = Beige
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
                            ) {
                                // 카테고리 & 제목
                                Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 8.dp).clickable { showCategoryDialog = true }) {
                                        Text(category.ifEmpty { "카테고리" }, fontWeight = FontWeight.Bold, color = if (category.isEmpty()) Color.Gray else Color.Black)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    TextField(
                                        value = title,
                                        onValueChange = { viewModel.updateTitle(it) },
                                        placeholder = { Text("글 제목") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                        singleLine = true
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                                // 날짜 선택
                                Row(modifier = Modifier.fillMaxWidth().clickable { showDatePickerDialog = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = Color.Gray)
                                    Spacer(Modifier.width(8.dp))
                                    val dateText = if (startDate != null && endDate != null) {
                                        "${SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(startDate!!))} ~ ${SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(endDate!!))}"
                                    } else "여행 기간을 선택해주세요"
                                    Text(dateText, color = if (startDate != null) Color.Black else Color.Gray)
                                }
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                                // 사진 첨부
                                Row(modifier = Modifier.fillMaxWidth().clickable { galleryLauncher.launch("image/*") }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CameraAlt, null, tint = Color.Gray)
                                    Spacer(Modifier.width(8.dp))
                                    val totalCount = groupedImages.values.flatten().size + images.size
                                    Text("사진 첨부하기 ($totalCount)", color = Color.Gray)
                                }

                                // 기존 사진 리스트
                                if (images.isNotEmpty()) {
                                    Text("기존 사진", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                                    LazyRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(images) { imageUrl ->
                                            Box(modifier = Modifier.size(100.dp)) {
                                                AsyncImage(
                                                    model = toFullUrl(imageUrl),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }

                                // 새로 추가한 사진 리스트
                                val newImages = groupedImages.values.flatten()
                                    .filter { it.uri.scheme == "content" || it.uri.scheme == "file" }
                                if (newImages.isNotEmpty()) {
                                    Text("새로 추가한 사진", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                                    LazyRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(newImages) { image ->
                                            Box(modifier = Modifier.size(100.dp)) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(image.uri),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }

                                TextField(
                                    value = tagsInput,
                                    onValueChange = { tagsInput = it },
                                    placeholder = { Text("#태그 입력") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )
                                HorizontalDivider()
                                TextField(
                                    value = content,
                                    onValueChange = { viewModel.updateContent(it) },
                                    placeholder = { Text("내용을 입력하세요...") },
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )
                                Spacer(Modifier.height(50.dp))
                            }
                        }
                    }
                }
            }
        }

        // 지도 팝업
        if (showMapDialog && mapDialogLocations.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).zIndex(10f).clickable { showMapDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f).height(400.dp).zIndex(11f).clickable(enabled = false) {},
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = mapDialogTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { showMapDialog = false }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "닫기") }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            val firstLocation = mapDialogLocations.first()
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition(LatLng(firstLocation.first, firstLocation.second), 14.0)
                            }
                            NaverMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(isZoomControlEnabled = true, isScrollGesturesEnabled = true, isZoomGesturesEnabled = true, isLogoClickEnabled = false)
                            ) {
                                mapDialogLocations.forEachIndexed { index, locationPair ->
                                    Marker(
                                        state = MarkerState(position = LatLng(locationPair.first, locationPair.second)),
                                        captionText = "사진 ${index + 1}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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