package com.example.travelapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.bottomColor

/**
 * 앱 전체에서 사용되는 하단 네비게이션 바
 *
 * @param navController 화면 전환을 위한 NavController
 * @param currentRoute 현재 활성화된 화면의 route (선택된 탭 표시용)
 *
 * [설계 의도]
 * - 재사용성: 모든 화면에서 동일한 BottomBar를 사용
 * - 상태 관리: currentRoute를 통해 현재 선택된 탭을 시각적으로 구분
 * - 확장성: 새로운 탭 추가 시 BottomNavItem만 추가하면 됨
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String
) {
    Row (
      modifier = Modifier
          .fillMaxWidth()
          .background(bottomColor)
          .navigationBarsPadding() // 시스템 네비게이션 바 영역만큼 패딩 추가
          .padding(horizontal = 8.dp)
          .height(60.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 각 네비게이션 아이템 정의
        // data class를 사용해 아이템 정보를 구조화
        val navItems = listOf(
            BottomNavItem(
                icon = Icons.Filled.Home,
                label = "메인 화면",
                route = Screen.Home.route
            ),
            BottomNavItem(
                icon = Icons.Filled.Person,
                label = "마이 페이지",
                route = Screen.MyPage.route
            )
        )

        navItems.forEach { item ->
            BottomNavItemView(
                item = item,
                isSelected = currentRoute == item.route,
                onClick = {
                    if(currentRoute != item.route) {
                        navController.navigate(item.route) {
                            launchSingleTop = true // 중복 스택 방지
                        }
                    }
                }
            )
        }
    }
}

/**
 * 하단 네비게이션 아이템 데이터 클래스
 *
 * [Kotlin 문법 설명]
 * data class: equals(), hashCode(), toString(), copy() 자동 생성
 * 불변(immutable) 데이터를 담는 용도로 적합
 */
private data class BottomNavItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

/**
 * 개별 네비게이션 아이템 UI
 *
 * @param item 아이템 정보
 * @param isSelected 현재 선택된 탭인지 여부
 * @param onClick 클릭 시 실행할 동작
 */
@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) {
                androidx.compose.ui.graphics.Color(0xFF1976D2)
            } else {
                androidx.compose.ui.graphics.Color(0xFF616161)
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            color = if (isSelected) {
                androidx.compose.ui.graphics.Color(0xFF1976D2)
            } else {
                androidx.compose.ui.graphics.Color(0xFF616161)
            }
        )
    }
}