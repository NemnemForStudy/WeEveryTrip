package com.example.travelapp.ui.write

import android.net.Uri // 추가
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult // 추가
import androidx.activity.result.contract.ActivityResultContracts // 추가
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // [수정] 호환성 좋은 기본 아이콘 사용
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.ui.theme.TravelAppTheme
import androidx.compose.foundation.interaction.MutableInteractionSource // 추가된 import
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WriteScreen(
    navController: NavController,
    viewModel: WriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current // Toast 메시지용

    // ViewModel의 게시물 생성 상태 관찰
    val postCreationStatus by viewModel.postCreationStatus.collectAsStateWithLifecycle()

    // 상태 관리 변수들
    var showDialog by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf("카테고리") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") } // tags 변수 이름을 tagsInput으로 변경하여 List<String>과 구분
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // 선택된 이미지 URI 상태

    // PostCreationStatus 변화에 따른 UI 피드백 처리
    LaunchedEffect(postCreationStatus) {
        when(postCreationStatus) {
            is WriteViewModel.PostCreationStatus.Success -> {
                val postId = (postCreationStatus as WriteViewModel.PostCreationStatus.Success).postId
                navController.popBackStack()
            }
            is WriteViewModel.PostCreationStatus.Error -> {
                val message = (postCreationStatus as WriteViewModel.PostCreationStatus.Error).message
            }
            is WriteViewModel.PostCreationStatus.Loading -> {
                Toast.makeText(context, "게시물 등록 중...", Toast.LENGTH_SHORT).show()
            }
            WriteViewModel.PostCreationStatus.Idle -> {
                // 초기 상태, 아무것도 안함.
            }
        }
    }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            Toast.makeText(context, "사진이 선택되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 1. 여행 유형 선택 팝업
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                // [수정] 팝업 바깥 눌렀을 때 앱 꺼짐 방지
                showDialog = false
            },
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

    // 2. 메인 화면 구성
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // [상단 바]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [수정] 호환성 문제 없는 기본 아이콘(Filled.ArrowBack) 사용
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { navController.popBackStack() }
                )

                // 등록 버튼
                Text(
                    text = "등록",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        // 등록 버튼 클릭 시 동작
                        if(title.isNotEmpty() && content.isNotEmpty() && category != "카테고리") {
                            // 태그 문자열을 List<String> 으로 변환(공백 기준)
                            val tagsList = tagsInput.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
                            //ViewModel createPost 함수 호출
                            viewModel.createPost(
                                category = category,
                                title = title,
                                content = content,
                                tags = tagsList,
                                imgUrl = selectedImageUri?.toString()
                            )
                        } else {
                            Toast.makeText(context, "카테고리, 제목, 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // [수정] HorizontalDivider -> Divider 로 변경 (앱 튕김의 주범 해결)
            HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // [카테고리 선택 & 제목 입력]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDialog = true }
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

            Spacer(modifier = Modifier.height(16.dp))

            // [사진 첨부 버튼]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { galleryLauncher.launch("image/*") } // 갤러리 열기
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "사진 첨부",
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "사진 첨부하기", color = Color.Gray)
            }

            // [수정] Divider 사용
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // [태그 입력]
            TextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                placeholder = { Text("#태그를 입력하세요") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            // [수정] Divider 사용
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // [본문 입력 (TextArea)]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 남은 공간 모두 채우기
            ) {
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            text = "불쾌감을 주는 욕설, 비하 발언 등 이상한 글을 쓸 경우\n이용 제재 및 처벌의 대상이 될 수 있습니다.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
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
}

// 미리보기용
@Preview(showBackground = true)
@Composable
fun WriteScreenPreview() {
    TravelAppTheme {
        val navController = rememberNavController()
        WriteScreen(navController = navController)
    }
}