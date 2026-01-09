package com.example.travelapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.data.api.SessionManager
import com.example.travelapp.ui.navigation.AppNavHost
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.util.TokenManager
import com.naver.maps.map.NaverMapSdk
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Hilt를 통해 TokenManager 인스턴스 주입받음.
    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var sessionManager: SessionManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientId = BuildConfig.NAVER_MAP_CLIENT_ID
        // 네이버 지도 SDK 클라이언트 ID 설정
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient(clientId)

        enableEdgeToEdge()

        setContent {
            TravelAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 네비게이션을 관리할 컨트롤러 생성
                    val navController = rememberNavController()

                    val isSessionValid by sessionManager.isSessionValid.collectAsState()

                    // 딥링크 처리 로직 추가
                    LaunchedEffect(isSessionValid) {
                        if(!isSessionValid) {
                            // 토큰 완전 삭제
                            tokenManager.clearAllTokens()
                            tokenManager.clearToken()

                            // 로그인 화면으로 강제 이동
                            navController.navigate(Screen.Login.route) {
                                // 현재 내비게이션 스택에 쌓인 모든 화면을 비움.
                                popUpTo(0) { inclusive = true }
                            }

                            // 다시 로그인 프로세스 위해 상태 초기화
                            sessionManager.resetSession()
                        }
                    }

                    LaunchedEffect(this@MainActivity.intent) {
                        handleDeepLink(this@MainActivity.intent, navController)
                    }
                    // 만들어둔 네비게이션 지도 넣고 전달
                    AppNavHost(navController = navController, tokenManager = tokenManager)
                }
            }
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(intent)
        // 새로 들어온 Intent로 교체해줘야 LaunchedEffect가 새로운 데이터를 인식합니다.
        setIntent(intent)
    }

    private fun handleDeepLink(intent: Intent?, navController: NavController) {
        intent?.data?.let { uri ->
            if(uri.host == "kakaolink") {
                val postId = uri.getQueryParameter("postId")
                if(!postId.isNullOrEmpty()) {
                    navController.navigate("postDetail/${postId}") {
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}