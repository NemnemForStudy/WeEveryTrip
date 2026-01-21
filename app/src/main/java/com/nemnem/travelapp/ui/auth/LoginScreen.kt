package com.nemnem.travelapp.ui.auth

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nemnem.travelapp.BuildConfig
import com.nemnem.travelapp.R
import com.nemnem.travelapp.ui.navigation.Screen
import com.nemnem.travelapp.ui.theme.Beige
import com.nemnem.travelapp.ui.theme.PointRed
import com.nemnem.travelapp.ui.theme.TextMain
import com.nemnem.travelapp.ui.theme.TextSub
import com.nemnem.travelapp.ui.viewModel.LoginEvent
import com.nemnem.travelapp.ui.viewModel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    // --- 1. 로직 관련 설정 ---
    val loginViewModel: LoginViewModel = hiltViewModel()
    val context = LocalContext.current
    val uiState by loginViewModel.loginUiState.collectAsStateWithLifecycle()
    val TAG = "LoginScreen"

    // 구글 로그인 클라이언트 설정
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
    Log.d("AuthDebug", "GOOGLE_WEB_CLIENT_ID : ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // 구글 로그인 결과 처리 런처
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            loginViewModel.handleGoogleSignInResult(task)
        } else {
            loginViewModel.emitLoginFailed("구글 로그인이 취소되었습니다")
        }
    }

    // 로그인 이벤트 관찰 (성공/실패 시 화면 이동 및 토스트)
    LaunchedEffect(Unit) {
        loginViewModel.loginEvent.collect { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is LoginEvent.LoginFailed -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- 2. 애니메이션 설정 ---
    val animStates = List(5) { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        animStates.forEachIndexed { index, animatable ->
            launch {
                delay(index * 100L)
                animatable.animateTo(1f, animationSpec = tween(800, easing = EaseOutBack))
            }
        }
    }

    // --- 3. UI 그리기 ---
    Box(modifier = Modifier.fillMaxSize().background(Beige)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp)
                .systemBarsPadding(), // 상태바, 내비바 영역 자동 확보
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // [상단] 브랜드 로고 영역
            Column(
                modifier = Modifier
                    .padding(top = 100.dp)
                    .alpha(animStates[0].value)
                    .offset(y = (20 * (1 - animStates[0].value)).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "ModuTrip",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain,
                        modifier = Modifier.offset(x = (-10).dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "나의 발자취가 지도가 되는 순간",
                    fontSize = 16.sp,
                    color = Color(0xFF616161),
                    fontWeight = FontWeight.Medium
                )
            }

            // [중간] 소셜 로그인 버튼 영역
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 네이버
                Box(modifier = Modifier.alpha(animStates[1].value).offset(y = (10 * (1 - animStates[1].value)).dp)) {
                    SocialLoginButton(
                        text = "네이버로 시작하기",
                        containerColor = Color(0xFF03C75A),
                        contentColor = Color.White,
                        iconRes = R.drawable.ic_naver_logo,
                        onClick = { loginViewModel.loginWithNaver(context) }
                    )
                }
                // 카카오
                Box(modifier = Modifier.alpha(animStates[2].value).offset(y = (10 * (1 - animStates[2].value)).dp)) {
                    SocialLoginButton(
                        text = "카카오톡으로 시작하기",
                        containerColor = Color(0xFFFEE500),
                        contentColor = Color(0xDB000000),
                        iconRes = R.drawable.ic_kakao_symbol,
                        onClick = { loginViewModel.loginWithKakaoTalk(context) }
                    )
                }
                // 구글
                Box(modifier = Modifier.alpha(animStates[3].value).offset(y = (10 * (1 - animStates[3].value)).dp)) {
                    SocialLoginButton(
                        text = "Google 계정으로 시작하기",
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F1F1F),
                        iconRes = R.drawable.ic_google_logo,
                        borderStroke = BorderStroke(1.dp, Color(0xFF747775)),
                        onClick = { googleAuthLauncher.launch(googleSignInClient.signInIntent) }
                    )
                }
            }

            // [하단] 푸터 영역
            Column(
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .alpha(animStates[4].value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "로그인 없이 둘러보기",
                    fontSize = 14.sp,
                    color = TextSub,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .clickable {
                            // 비회원 둘러보기 로직 (필요시 홈으로 바로 이동)
                            navController.navigate(Screen.Home.route)
                        }
                )
                Text(
                    text = "로그인 시 이용약관 및 개인정보처리방침에 동의하게 됩니다.",
                    fontSize = 11.sp,
                    color = Color(0xFFBDBDBD),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 로딩 화면 (중앙 프로그레스 바)
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PointRed)
            }
        }
    }
}

/**
 * [DRY 원칙] 공통 소셜 로그인 버튼 컴포넌트
 */
@Composable
fun SocialLoginButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    iconRes: Int,
    borderStroke: BorderStroke? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(27.dp), // Pill 모양
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        border = borderStroke,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}