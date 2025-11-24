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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.filled.Close
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val postCreationStatus by viewModel.postCreationStatus.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf("카테고리") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    // [수정] 단일 이미지가 아닌, 여러 이미지 Uri를 저장할 리스트로 변경합니다.
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(postCreationStatus) {
        when (val status = postCreationStatus) {
            is WriteViewModel.PostCreationStatus.Success -> {
                Toast.makeText(context, "게시물이 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
                // 상태를 초기화하고 이전 화면으로 돌아갑니다.
                viewModel.resetStatus()
                navController.popBackStack()
            }
            is WriteViewModel.PostCreationStatus.Error -> {
                Toast.makeText(context, "오류: ${status.message}", Toast.LENGTH_LONG).show()
                viewModel.resetStatus() // 오류 발생 후에도 상태 초기화
            }
            is WriteViewModel.PostCreationStatus.Loading -> {
                // 로딩 중임을 알리는 UI를 여기에 추가할 수 있습니다. (예: 로딩 스피너)
            }
            WriteViewModel.PostCreationStatus.Idle -> { /* 초기 상태 */ }
        }
    }

    // [수정] 여러 이미지를 선택할 수 있는 GetMultipleContents()로 변경합니다.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // 선택된 이미지 리스트를 상태에 업데이트합니다.
        selectedImageUris = uris
        if (uris.isNotEmpty()) {
            Toast.makeText(context, "${uris.size}개의 사진이 선택되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "여행 유형 선택") },
            text = { Text(text = "작성할 글의 여행 유형을 선택해주세요.") },
            dismissButton = {
                TextButton(onClick = {
                    category = "국내여행"
                    showDialog = false
                }) { Text("국내여행") }
            },
            confirmButton = {
                TextButton(onClick = {
                    category = "국외여행"
                    showDialog = false
                }) { Text("국외여행") }
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
                            // [수정] ViewModel에 이미지 Uri 리스트를 전달합니다.
                            viewModel.createPost(
                                category = category,
                                title = title,
                                content = content,
                                tags = tagsList,
                                imgUris = selectedImageUris
                            )
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
        ) {
            // [카테고리 선택 & 제목 입력]
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

            // [사진 첨부 버튼]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { galleryLauncher.launch("image/*") }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = "사진 첨부", tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "사진 첨부하기 (${selectedImageUris.size})", color = Color.Gray)
            }

            // [추가] 선택된 이미지 미리보기
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier.size(100.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "선택된 이미지",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    // 'x' 버튼 클릭 시 해당 이미지 리스트에서 제거
                                    selectedImageUris = selectedImageUris - uri
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "이미지 제거", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
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
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WriteScreenPreview() {
    TravelAppTheme {
        WriteScreen(navController = rememberNavController())
    }
}