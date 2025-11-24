package com.example.travelapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.ui.navigation.AppNavHost
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.util.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Hilt를 통해 TokenManager 인스턴스 주입받음.
    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TravelAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. 네비게이션을 관리할 컨트롤러 생성
                    val navController = rememberNavController()

                    // 2. 만들어둔 네비게이션 지도 넣고 전달
                    AppNavHost(navController = navController, tokenManager = tokenManager)
                }
            }
        }
    }
}