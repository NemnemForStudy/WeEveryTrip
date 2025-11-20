package com.example.travelapp.ui.auth

import android.app.Activity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.travelapp.BuildConfig
import com.example.travelapp.R
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.ui.viewModel.LoginEvent
import com.example.travelapp.ui.viewModel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(navController: NavController) {
    val loginViewModel: LoginViewModel = viewModel()
    val context = LocalContext.current
    val TAG = "LoginViewModel"

    // GoogleSignInClient 초기화
    // 웹 클라이언트 ID 사용해 구글 로그인 옵션 설정
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient = remember(context) { GoogleSignIn.getClient(context, gso) }

    // Google 로그인 결과를 처리하기 위한 ActivityResultLauncher
    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                loginViewModel.handlerGoogleSignInResult(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Google 로그인 실패", e)
            }
        }
    }

    // 로그인 이벤트 관찰 및 처리
    LaunchedEffect(key1 = Unit) {
        loginViewModel.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> {
                    Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is LoginEvent.LoginFailed -> {
                    Toast.makeText(context, "로그인 실패: ${event.message}", Toast.LENGTH_SHORT).show()
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
                text = "모여로그",
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // 버튼 사이 간격 16dp
        ) { // 실제 카카오 구글 로그인 버튼 이미지 교체 예정
            Button(
                onClick = { loginViewModel.loginWithNaver(context) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Image(
                    painter = painterResource(id = R.drawable.naver_login_button),
                    contentDescription = "네이버 로그인",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Button(
                onClick = { loginViewModel.loginWithKakaoTalk(context) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Image(
                    painter = painterResource(id = R.drawable.kakao_login_medium_narrow),
                    contentDescription = "카카오 로그인",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Button(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleAuthLauncher.launch(signInIntent)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Image(
                    painter = painterResource(id = R.drawable.android_light_sq2),
                    contentDescription = "구글 로그인",
                    modifier = Modifier.fillMaxSize()
                )
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