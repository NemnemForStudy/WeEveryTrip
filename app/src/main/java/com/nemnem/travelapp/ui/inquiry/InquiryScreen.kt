package com.nemnem.travelapp.ui.inquiry

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nemnem.travelapp.ui.theme.Beige
import com.nemnem.travelapp.ui.theme.PointRed
import com.nemnem.travelapp.ui.theme.TextDisabled
import com.nemnem.travelapp.ui.theme.TextMain
import com.nemnem.travelapp.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryScreen(
    navController: NavController,
    userEmail: String,
    viewModel: InquiryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
//    val db = remember { FirebaseFirestore.getInstance() }
//    val auth = remember { FirebaseAuth.getInstance() }

    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("ㄴ : ", "${userEmail}")
        viewModel.onEmailChanged(userEmail)
    }
    LaunchedEffect(uiState) {
        when(uiState) {
            is InquiryUiState.Success -> {
                Toast.makeText(context, "문의가 성공적으로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            is InquiryUiState.Error -> {
                val message = (uiState as InquiryUiState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

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
                onValueChange = { viewModel.onTitleChanged(it) },
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
                onValueChange = { viewModel.onContentChanged(it) },
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
                    viewModel.sendEmail()
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
                if(uiState is InquiryUiState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
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
}