package com.example.travelapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.travelapp.ui.auth.LoginScreen
import com.example.travelapp.ui.auth.SplashScreen
import com.example.travelapp.ui.home.FeedScreen
import com.example.travelapp.ui.home.HomeScreen
import com.example.travelapp.ui.write.WriteScreen
import com.example.travelapp.util.TokenManager

/**
 * 앱의 화면 주소(Route)를 정의하는 sealed class입니다.
 * sealed class를 사용하면, 앱에 어떤 화면들이 있는지 한눈에 파악하기 쉽고
 * 주소를 잘못 입력하는 실수를 방지할 수 있습니다.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Write : Screen("write") // 글쓰기 화면 경로 추가
    object Feed : Screen("feed") // 계시판 화면 경로
}

/**
 * 앱의 전체 내비게이션 '지도' 역할을 하는 Composable 입니다.
 * NavController를 받아서, 어떤 주소(route)로 요청이 오면
 * 어떤 화면(Composable)을 보여줄지 정의합니다.
 *
 * @param navController 화면 이동을 제어하는 컨트롤러
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    tokenManager: TokenManager
) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onTimeout = {

//                 서버 있는 곳에서 환경
//                 스플래시 화면에서 화면 종료 시, 토큰 유효성을 검사해 분기함.
                val destination = if(tokenManager.isTokenValid()) Screen.Home.route else Screen.Login.route
                navController.navigate(destination) {
                    // 스플래시 화면을 백스택에서 제거하여 뒤로가기 시 스플래시 화면으로 돌아가지 않도록 합니다.
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
                // 서버 없는 곳에서 환경 변경
//                navController.navigate(Screen.Write.route) {
//                    popUpTo(Screen.Splash.route) { inclusive = true }
//                }
            })
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Write.route) { // 글쓰기 화면 composable 추가
            WriteScreen(navController = navController)
        }

        composable(Screen.Feed.route) {
            FeedScreen(
                onPostClick = { post ->
                    // TODO: 게시물 상세 화면 이동
                    // navController.navigate(Screen.Detail.route)

                }
            )
        }
    }
}