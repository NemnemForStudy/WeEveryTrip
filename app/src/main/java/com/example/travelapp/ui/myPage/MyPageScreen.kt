package com.example.travelapp.ui.myPage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.ui.components.BottomNavigationBar
import com.example.travelapp.ui.theme.Beige

// 1. [로직 담당] ViewModel 연결하고 데이터 준비하는 곳
@Composable
fun MyPageScreen(
    navController: NavController,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    // 임시 데이터 (나중에 ViewModel 데이터로 교체)
    val nickname = "여행자 (DB연동)"
    val email = "traveler@example.com"
    val postCount = 0
    val likeCount = 0
    val commentCount = 0

    // 이벤트 함수 준비
    val onEditProfileClick = { /* viewModel.onEditProfile() */ }
    val onLogoutClick = { /* viewModel.logout() */ }

    // 2. [UI 담당] MyPageContent를 호출합니다. (자기 자신이 아닙니다!)
    MyPageContent(
        navController = navController,
        nickname = nickname,
        email = email,
        postCount = postCount,
        likeCount = likeCount,
        commentCount = commentCount,
        onEditProfileClick = onEditProfileClick,
        onLogoutClick = onLogoutClick
    )
}

// 3. [그림 담당] 실제 화면을 그리는 곳 (이름이 Content 입니다!)
@Composable
fun MyPageContent(
    navController: NavController,
    nickname: String,
    email: String,
    postCount: Int,
    likeCount: Int,
    commentCount: Int,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            // NavHostController인 경우에만 바텀 네비게이션 표시
            if (navController is NavHostController) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = "mypage"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Beige)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileSection(
                nickname = nickname,
                email = email,
                onEditClick = onEditProfileClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActivityStatsCard(
                postCount = postCount,
                likeCount = likeCount,
                commentCount = commentCount
            )

            Spacer(modifier = Modifier.height(24.dp))

            MenuSection(
                onMyPostsClick = {},
                onLikedPostsClick = { },
                onNotificationClick = { },
                onSettingsClick = { },
                onHelpClick = { },
                onLogoutClick = onLogoutClick
            )
        }
    }
}

// --- 아래 하위 컴포넌트들은 그대로 사용 ---

@Composable
private fun ProfileSection(
    nickname: String,
    email: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD))
                    .border(2.dp, Color(0xFF1976D2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "프로필",
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF5F5F5), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "프로필 수정",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivityStatsCard(
    postCount: Int,
    likeCount: Int,
    commentCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(count = postCount, label = "게시글")
            StatDivider()
            StatItem(count = likeCount, label = "좋아요")
            StatDivider()
            StatItem(count = commentCount, label = "댓글")
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color(0xFFE0E0E0))
    )
}

@Composable
private fun MenuSection(
    onMyPostsClick: () -> Unit,
    onLikedPostsClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Text(
                text = "내 활동",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            MenuItem(Icons.Outlined.Article, "내가 쓴 글", onClick = onMyPostsClick)
            MenuItem(Icons.Default.Favorite, "좋아요한 글", Color(0xFFE91E63), onClick = onLikedPostsClick)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            Text(
                text = "설정",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            MenuItem(Icons.Default.Notifications, "알림 설정", onClick = onNotificationClick)
            MenuItem(Icons.Default.Settings, "앱 설정", onClick = onSettingsClick)
            MenuItem(Icons.Outlined.Help, "도움말", onClick = onHelpClick)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            MenuItem(Icons.Outlined.Logout,
                "로그아웃",
                Color(0xFFE53935),
                Color(0xFFE53935),
                onClick = onLogoutClick
            )

            Spacer(modifier = Modifier.height(8.dp))

        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = Color(0xFF666666),
    titleColor: Color = Color(0xFF333333),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(20.dp)
        )
    }
}

// 4. [프리뷰] MyPageContent(껍데기)를 보여줌 -> Hilt 에러 없음!
@Preview(showBackground = true, heightDp = 800)
@Composable
fun MyPageScreenPreview() {
    MyPageContent(
        navController = rememberNavController(),
        nickname = "프리뷰 유저",
        email = "preview@test.com",
        postCount = 10,
        likeCount = 20,
        commentCount = 5,
        onEditProfileClick = {},
        onLogoutClick = {}
    )
}