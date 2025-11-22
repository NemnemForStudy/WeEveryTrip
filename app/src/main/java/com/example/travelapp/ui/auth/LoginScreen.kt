package com.example.travelapp.ui.auth

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlin.contracts.contract

@Composable
fun LoginScreen(navController: NavController) {
    val loginViewModel: LoginViewModel = viewModel()
    val context = LocalContext.current
    val TAG = "LoginViewModel"

    // GoogleSignInClient 초기화
    // 웹 클라이언트 ID 사용해 구글 로그인 옵션 설정
    Log.d(TAG, "클라이언트 ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient = remember(context) { GoogleSignIn.getClient(context, gso) }

    // Google 로그인 결과를 처리하기 위한 ActivityResultLauncher
    val googleAuthLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "구글 로그인 결과 코드: ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "구글 로그인 결과 OK - 계정 정보 처리 시작")
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            loginViewModel.handlerGoogleSignInResult(task)
        } else {
            Log.w(TAG, "구글 로그인 취소 또는 실패 - 결과 코드: ${result.resultCode}")
            loginViewModel.emitLoginFailed("구글 로그인이 취소되었습니다")
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
        ) {
            NaverLoginButton(onClick = { loginViewModel.loginWithNaver(context) })
            KakaoLoginButton(onClick = { loginViewModel.loginWithKakaoTalk(context) })
            GoogleLoginButton(
                onClick = {
                    Log.d(TAG, "구글 로그인 버튼 클릭")
                    try {
                        val signInIntent = googleSignInClient.signInIntent
                        Log.d(TAG, "인텐트 생성 성공")
                        googleAuthLauncher.launch(signInIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "생성 실패", e)
                        loginViewModel.emitLoginFailed("초기화 실패")
                    }
                }
            )
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

@Composable
fun NaverLoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03C75A)), // 컨테이너 색상 #03C75A
        contentPadding = PaddingValues(horizontal = 20.dp) // 좌우 패딩
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center // 심볼과 레이블 중앙 정렬
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_naver_logo), // 네이버 심볼 (Path로 그린 XML)
                contentDescription = "네이버 로그인",
                modifier = Modifier.size(30.dp) // 심볼 크기
            )
            Spacer(modifier = Modifier.width(8.dp)) // 심볼과 텍스트 사이 간격
            Text(
                text = "네이버 로그인", // 완성형 레이블
                color = Color.White, // 레이블 색상 WHITE
                fontSize = 17.sp, // 폰트 크기
                fontWeight = FontWeight.Bold // 폰트 두께
            )
        }
    }
}

@Composable
fun KakaoLoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp), // 버튼의 최소 높이를 고려하여 50dp로 설정
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE500)), // 컨테이너 색상 #FEE500
        contentPadding = PaddingValues(horizontal = 20.dp) // 좌우 패딩으로 심볼과 텍스트가 너무 가장자리에 붙지 않도록
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center // 심볼과 레이블을 함께 중앙 정렬
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_kakao_symbol), // 카카오 심볼 (Path로 그린 XML)
                contentDescription = "카카오 심볼",
                modifier = Modifier.size(30.dp) // 심볼 크기
            )
            Spacer(modifier = Modifier.width(8.dp)) // 심볼과 텍스트 사이 간격
            Text(
                text = "카카오 로그인", // 완성형 레이블
                color = Color(0xDB000000), // 레이블 색상 #000000 85%
                fontSize = 17.sp, // 폰트 크기
                fontWeight = FontWeight.Bold // OS 기본 시스템 서체를 사용하므로 Bold로 강조
            )
        }
    }
}

@Composable
fun GoogleLoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp), // 카카오 버튼과 동일한 높이
        shape = RoundedCornerShape(12.dp), // 카카오 버튼과 동일한 radius
        colors = ButtonDefaults.buttonColors(containerColor = Color.White), // 컨테이너 색상 WHITE
        border = BorderStroke(1.dp, Color(0xFF747775)), // 테두리 1px solid #747775
        contentPadding = ButtonDefaults.ContentPadding // 기본 패딩 사용
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center // 심볼과 레이블 중앙 정렬
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo), // 구글 심볼 (기존 XML)
                contentDescription = "구글 로그인",
                modifier = Modifier.size(20.dp) // 심볼 크기 20px
            )
            Spacer(modifier = Modifier.width(12.dp)) // 심볼과 텍스트 사이 간격 12px
            Text(
                text = "Google 계정으로 로그인", // 레이블 텍스트
                color = Color(0xFF1F1F1F), // 텍스트 색상 #1f1f1f
                fontSize = 14.sp, // 폰트 크기 14px
                fontWeight = FontWeight.Medium // 폰트 두께 500
            )
        }
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
