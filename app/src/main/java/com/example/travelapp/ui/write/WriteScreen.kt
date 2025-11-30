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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.util.ExifUtils

/**
 * 1. [Stateful] WriteScreen
 * - 실제 앱에서 사용되는 진입점입니다.
 * - HiltViewModel을 주입받고, 상태(State)를 수집해서 Content에 넘겨줍니다.
 */
@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // ViewModel 상태 구독
    val postCreationStatus by viewModel.postCreationStatus.collectAsStateWithLifecycle()
    val latitude by viewModel.latitude.collectAsStateWithLifecycle()
    val longitude by viewModel.longitude.collectAsStateWithLifecycle()

    // Side Effect 처리
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

    // 2. [Stateless] Content 호출
    // ViewModel 자체를 넘기지 않고, 필요한 데이터와 함수만 쏙쏙 뽑아서 넘깁니다.
    WriteScreenContent(
        navController = navController,
        postCreationStatus = postCreationStatus,
        latitude = latitude,
        longitude = longitude,
        onUpdateLocation = viewModel::updateLocation,
        onCreatePost = viewModel::createPost,
        onResetStatus = viewModel::resetStatus
    )
}

/**
 * 2. [Stateless] WriteScreenContent
 * - ViewModel 의존성이 전혀 없는 순수한 UI입니다.
 * - 프리뷰에서도 이 함수를 호출하면 에러 없이 화면을 볼 수 있습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreenContent(
    navController: NavController,
    postCreationStatus: WriteViewModel.PostCreationStatus,
    latitude: Double?,
    longitude: Double?,
    onUpdateLocation: (Double, Double) -> Unit,
    onCreatePost: (String, String, String, List<String>, List<Uri>) -> Unit,
    onResetStatus: () -> Unit
) {
    val context = LocalContext.current

    // UI 상태 관리 (여기서만 쓰는 임시 데이터들)
    var showDialog by remember { mutableStateOf(true) }
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

            // EXIF 추출 로직
            val firstLocation = uris.asSequence()
                .mapNotNull { ExifUtils.extractLocation(context, it) }
                .firstOrNull()

            if (firstLocation != null) {
                // ViewModel 함수 대신 파라미터로 받은 함수 호출
                onUpdateLocation(firstLocation.first, firstLocation.second)
                Toast.makeText(context, "사진 위치 정보를 불러왔습니다!", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    TextButton(onClick = {
                        if (title.isNotEmpty() && content.isNotEmpty() && category != "카테고리") {
                            val tagsList = tagsInput.split(" ", ",", "#").map { it.trim() }.filter { it.isNotEmpty() }
                            // 파라미터로 받은 함수 호출
                            onCreatePost(category, title, content, tagsList, selectedImageUris)
                        } else {
                            Toast.makeText(context, "카테고리, 제목, 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("등록", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
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

            // [사진 첨부]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { galleryLauncher.launch("image/*") }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "사진 첨부", tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "사진 첨부하기 (${selectedImageUris.size})", color = Color.Gray)
            }

            // [이미지 미리보기]
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(100.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "선택된 이미지",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "제거", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // ⭐️ [지도 미리보기 버튼 영역]
            if (latitude != null && longitude != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "위치 감지",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        // 0.0으로 초기화하는 등 실제 삭제 로직 호출 가능 (현재는 UI만)
                        // onUpdateLocation(0.0, 0.0) // 필요 시 주석 해제
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "위치 삭제",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        navController.navigate("map?lat=$latitude&lon=$longitude")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📍 지도에서 위치 미리보기")
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if(selectedImageUris.isNotEmpty()) {
                // 사진은 선택했는데 위치 정보가 없는 경우
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "정보",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "선택한 사진에 위치 정보가 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // [태그 입력]
            TextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                placeholder = { Text("#태그를 입력하세요 (쉼표, 띄어쓰기로 구분)") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // [본문 입력]
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("내용을 입력하세요...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
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

@Preview(showBackground = true)
@Composable
fun WriteScreenPreview() {
    TravelAppTheme {
        WriteScreenContent(
            navController = rememberNavController(),
            postCreationStatus = WriteViewModel.PostCreationStatus.Idle,
            latitude = 37.5665, // 프리뷰용 더미 데이터
            longitude = 126.9779,
            onUpdateLocation = { _, _ -> },
            onCreatePost = { _, _, _, _, _ -> },
            onResetStatus = {}
        )
    }
}