package com.example.travelapp.ui.inquiry

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.travelapp.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // ✅ DRY 원칙: 반복되는 TextField 색상 설정을 하나로 묶음
    // TextMain이 검정색(#000000)이므로 모든 텍스트가 아주 진하게 나옵니다.
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PointRed,       // 포커스 시 테두리
        unfocusedBorderColor = TextMain,     // 평상시 테두리 (진하게)
        focusedLabelColor = PointRed,        // 포커스 시 라벨
        unfocusedLabelColor = TextMain,      // 평상시 라벨 (진하게)
        cursorColor = PointRed,              // 커서 색상
        focusedTextColor = TextMain,         // 입력 텍스트
        unfocusedTextColor = TextMain,       // 입력 완료 텍스트
        focusedPlaceholderColor = TextMain.copy(alpha = 0.4f), // 힌트는 살짝 연하게 (구분용)
        unfocusedPlaceholderColor = TextMain.copy(alpha = 0.4f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("문의하기", style = Typography.titleLarge, color = TextMain) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Beige)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Beige)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 안내 문구 (TextSub 대신 TextMain 사용)
            Text(
                text = "앱 이용 중 불편한 점이나\n제안하고 싶은 내용을 남겨주세요.",
                style = Typography.bodyLarge,
                color = TextMain, // 뚜렷한 검정색 적용
                fontWeight = FontWeight.Bold // 조금 더 강조하고 싶다면 Bold 추가
            )

            // 2. 제목 입력란
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("제목") },
                placeholder = { Text("제목을 입력해주세요") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors, // 공통 설정 적용
                singleLine = true
            )

            // 3. 내용 입력란
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("내용") },
                placeholder = { Text("문의하실 내용을 상세히 적어주시면 큰 도움이 됩니다.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors // 공통 설정 적용
            )

            // 4. 보내기 버튼
            Button(
                onClick = {
                    if(title.isNotBlank() && content.isNotBlank()) {
                        isSubmitting = true
                        val inquiryData = hashMapOf(
                            "title" to title,
                            "content" to content,
                            "userEmail" to (auth.currentUser?.email ?: "anonymous"),
                            "userId" to (auth.currentUser?.uid ?: ""),
                            "timestamp" to Timestamp.now()
                        )

                        db.collection("inquiries")
                            .add(inquiryData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "문의가 성공적으로 접수되었습니다.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                isSubmitting = false
                                Toast.makeText(context, "전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = title.isNotBlank() && content.isNotBlank() && !isSubmitting,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PointRed,
                    disabledContainerColor = TextDisabled // 비활성화 시엔 라이트 그레이
                ),
            ) {
                Text(
                    text = "보내기",
                    style = Typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}