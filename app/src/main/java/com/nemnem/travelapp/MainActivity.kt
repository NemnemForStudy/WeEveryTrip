package com.nemnem.travelapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nemnem.travelapp.data.api.SessionManager
import com.nemnem.travelapp.ui.navigation.AppNavHost
import com.nemnem.travelapp.ui.navigation.Screen
import com.nemnem.travelapp.ui.theme.TravelAppTheme
import com.nemnem.travelapp.util.TokenManager
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
//                    val isSessionValid by sessionManager.isSessionValid.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {

                        // 기존 앱 화면 (바닥 레이어)
                        AppNavHost(navController = navController, tokenManager = tokenManager)
                    }

                    LaunchedEffect(intent) {
                        handleDeepLink(intent, navController)
                    }
                    // 만들어둔 네비게이션 지도 넣고 전달
//                    AppNavHost(navController = navController, tokenManager = tokenManager)
                }
            }
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        // 새로 들어온 Intent로 교체해줘야 LaunchedEffect가 새로운 데이터를 인식합니다.
        setIntent(newIntent)
    }

    private fun handleDeepLink(intent: Intent?, navController: NavController) {
        intent?.data?.let { uri ->
            if(uri.host == "kakaolink") {
                val postId = uri.getQueryParameter("postId")
                if(!postId.isNullOrEmpty()) {
                    // "postDetail/" 대신 AppNavHost에 정의된 "detail/" 경로를 사용합니다.
                    navController.navigate("detail/${postId}") {
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}
