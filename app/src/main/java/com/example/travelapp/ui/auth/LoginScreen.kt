package com.example.travelapp.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.travelapp.R
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.ui.viewModel.LoginEvent
import com.example.travelapp.ui.viewModel.LoginViewModel

@Composable
fun LoginScreen(navController: NavController) {
    val loginViewModel: LoginViewModel = viewModel()
    val context = LocalContext.current

    // 로그인 이벤트 관찰 및 처리
    LaunchedEffect(key1 = Unit) {
        loginViewModel.loginEvent.collect {
            when (it) {
                is LoginEvent.LoginSuccess -> {
                    Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is LoginEvent.LoginFailed -> {
                    Toast.makeText(context, "로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 화면 전체를 채우는 Column. 내부 요소들을 배치하는 기본 컨테이너
    Column (
        modifier = Modifier.fillMaxSize().padding(24.dp), // 화면 전체에 여백
        horizontalAlignment = Alignment.CenterHorizontally // 자식들을 가로측 중앙에 정렬
    ) {
        // 1. 상단 로고 영역
        // Spacer에 weight(1f)를 줘 로고를 화면 상단에 밀어 올리는 효과
        Spacer(modifier = Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.Center) { // 로고 텍스트들을 중간으로 정렬
            Text(
                text = "우리",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "모두의",
                style = MaterialTheme.typography.headlineLarge
            )
            Row { 
                Spacer(modifier = Modifier.width(30.dp))
                Text(
                    text = "여행",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // 버튼 사이 간격 16dp
        ) { // 실제 카카오 구글 로그인 버튼 이미지 교체 예정
            Button(
                onClick = { /* 네이버 로그인 클릭 시 동작 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("네이버 로그인")
            }
            Button(
                onClick = { loginViewModel.loginWithKakaoTalk(context) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Image(
                    // 네이밍은 무조건 소문자인듯함.
                    painter = painterResource(id = R.drawable.kakao_login_button),
                    contentDescription = "카카오 로그인 버튼",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Button(
                onClick = { /* 구글 로그인 클릭 시 동작 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("구글 로그인")
            }
        }

        // 하단 정책 안내 텍스트 영역
        // 마지막 Spacer가 로그인 버튼과 정책 텍스를 화면 아래쪽으로 밀어냄
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "로그인 시 서비스 이용 약관 및 개인정보 처리 방침에 동의하게 됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    TravelAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoginScreen(navController = NavController(LocalContext.current))
        }
    }
}