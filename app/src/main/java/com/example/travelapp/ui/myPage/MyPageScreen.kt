package com.example.travelapp.ui.myPage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.travelapp.ui.components.BottomNavigationBar
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.Beige
import com.example.travelapp.ui.theme.TextSub

// 1. [로직 담당] ViewModel 연결하고 데이터 준비하는 곳
@Composable
fun MyPageScreen(
    navController: NavController,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 닉네임 수정 다이얼로그 상태
    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var tempNickname by remember { mutableStateOf("") }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfile(context, null, it) } // 이미지 변경 시 바로 업로드.
    }
    LaunchedEffect(Unit) {
        viewModel.fetchUserInfo()
    }
    // 임시 데이터 (나중에 ViewModel 데이터로 교체)
    val currentUser = userState
    val nickname = currentUser?.nickname ?: "로딩 중"
    val email = currentUser?.email ?: ""
    val postCount = currentUser?.postCount ?: 0
    val likeCount = currentUser?.likeCount ?: 0
    val commentCount = currentUser?.commentCount ?: 0

    val pushActivity = currentUser?.pushActivity ?: true
    val pushMarketing = currentUser?.pushMarketing ?: false

    // 이벤트 함수 준비
    val onEditProfileClick = { /* viewModel.onEditProfile() */ }
    val onLogoutClick = { viewModel.logout(navController) }
    val profileUrl = currentUser?.profileImageUrl
    val localUri by viewModel.localImageUri.collectAsState()
    val imageVersion by viewModel.imageVersion.collectAsState()

    if(showEditNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNicknameDialog = false },
            title = { Text("닉네임 변경") },
            text = {
                OutlinedTextField(
                    value = tempNickname,
                    onValueChange = { tempNickname = it },
                    label = { Text("새 닉네임") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateProfile(context, tempNickname, null)
                        showEditNicknameDialog = false
                    },
                    // ✅ 현재 닉네임과 똑같거나 비어있으면 클릭 방지
                    enabled = tempNickname.isNotBlank() && tempNickname != nickname
                ) {
                    Text("변경")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNicknameDialog = false }) { Text("취소") }
            }
        )

    }
    if(showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "회원 탈퇴", fontWeight = FontWeight.Bold) },
            text = { Text(text = "정말 탈퇴하시겠습니까?\n작성하신 모든 게시글 정보는 숨김 처리됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.withdraw(navController) // 실제 탈퇴 로직 호출
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))
                ) { Text("탈퇴") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = TextSub)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 2. [UI 담당] MyPageContent를 호출합니다. (자기 자신이 아닙니다!)
    MyPageContent(
        navController = navController,
        nickname = nickname,
        email = email,
        profileUrl = profileUrl,
        localUri = localUri,
        imageVersion = imageVersion,
        postCount = postCount,
        likeCount = likeCount,
        commentCount = commentCount,

        pushActivity = pushActivity,
        pushMarketing = pushMarketing,

        onActivityChange = { isChecked ->
            viewModel.updateNotificationSetting("activity", isChecked)
        },
        onMarketingChange = { isChecked ->
            viewModel.updateNotificationSetting("marketing", isChecked)
        },

        onEditProfileClick = onEditProfileClick,
        onLogoutClick = { viewModel.logout(navController) },
        onDeleteAccountClick = { showDeleteDialog = true }, // 버튼 클릭시 팝업 창
        onEditNicknameClick = {
            tempNickname = nickname
            showEditNicknameDialog = true
        },
        onEditImageClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    )
}

// 3. [그림 담당] 실제 화면을 그리는 곳 (이름이 Content 입니다!)
@Composable
fun MyPageContent(
    navController: NavController,
    nickname: String,
    email: String,
    profileUrl: String?,
    localUri: Uri?,
    imageVersion: Long,
    postCount: Int,
    likeCount: Int,
    commentCount: Int,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    pushActivity: Boolean = true, // 기본값 임시 설정
    pushMarketing: Boolean = false,
    onActivityChange: (Boolean) -> Unit = {},
    onMarketingChange: (Boolean) -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onEditNicknameClick: () -> Unit,
    onEditImageClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            // NavHostController인 경우에만 바텀 네비게이션 표시
            if (navController is NavHostController) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = Screen.MyPage.route
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
                profileUrl = profileUrl,
                localUri = localUri,
                imageVersion = imageVersion,
                onEditNicknameClick = onEditNicknameClick,
                onEditImageClick = onEditImageClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActivityStatsCard(
                postCount = postCount,
                likeCount = likeCount,
                commentCount = commentCount
            )

            Spacer(modifier = Modifier.height(24.dp))

            MenuSection(
                navController = navController,
                pushActivity = pushActivity,
                pushMarketing = pushMarketing,
                onActivityChange = onActivityChange,
                onMarketingChange = onMarketingChange,
                onMyPostsClick = {},
                onLikedPostsClick = { },
                onSettingsClick = { },
                onHelpClick = { },
                onLogoutClick = onLogoutClick,
                onDeleteAccountClick = onDeleteAccountClick
            )
        }
    }
}

// --- 아래 하위 컴포넌트들은 그대로 사용 ---

@Composable
private fun ProfileSection(
    nickname: String,
    email: String,
    profileUrl: String?,
    localUri: Uri?,
    imageVersion: Long,
    onEditNicknameClick: () -> Unit,
    onEditImageClick: () -> Unit
) {

    val imageModel = remember(profileUrl, localUri, imageVersion) {
        if(localUri != null) {
            localUri // 사용자가 방금 고른 사진 (즉시 반영)
        } else if(!profileUrl.isNullOrEmpty()) {
            "$profileUrl?v=$imageVersion" // 서버 사진 (성공 시에만 주소 변경)
        } else {
            null
        }
    }
    // 배경을 흰색으로 하고 하단에만 살짝 그림자를 줍니다.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally // 중앙 정렬
    ) {
        // 1. 프로필 이미지와 카메라 버튼
        Box(
            modifier = Modifier
                .size(110.dp) // 크기를 더 키워 시원하게 보이게 함
                .clickable { onEditImageClick() },
            contentAlignment = Alignment.BottomEnd
        ) {
            if (!profileUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFEEEEEE), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 이미지가 없을 때의 배경
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F9FA), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xFFDEE2E6)
                    )
                }
            }

            // 📷 카메라 아이콘 배지
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = Color(0xFF1976D2),
                border = BorderStroke(2.dp, Color.White) // 흰색 테두리로 구분감 생성
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 닉네임 섹션
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 32.dp) // 아이콘 때문에 생기는 불균형 해소
        ) {
            Text(
                text = nickname,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212529)
            )
            IconButton(
                onClick = onEditNicknameClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "수정",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFADB5BD)
                )
            }
        }

        // 3. 이메일 텍스트
        Text(
            text = email,
            fontSize = 14.sp,
            color = Color(0xFF6C757D)
        )
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
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 20.dp),
//            horizontalArrangement = Arrangement.SpaceEvenly
//        ) {
//            StatItem(count = postCount, label = "게시글")
//            StatDivider()
//            StatItem(count = likeCount, label = "좋아요")
//            StatDivider()
//            StatItem(count = commentCount, label = "댓글")
//        }
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
            color = Color(0xFF616161)
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
    navController: NavController,
    pushActivity: Boolean,
    pushMarketing: Boolean,
    onActivityChange: (Boolean) -> Unit,
    onMarketingChange: (Boolean) -> Unit,
    onMyPostsClick: () -> Unit,
    onLikedPostsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit // 탈퇴 처리 함수 추가
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
//            Text(
//                text = "내 활동",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color(0xFF616161),
//                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
//            )

//            MenuItem(Icons.Outlined.Article, "내가 쓴 글", onClick = onMyPostsClick)
//            MenuItem(Icons.Default.Favorite, "좋아요한 글", Color(0xFFE91E63), onClick = onLikedPostsClick)

//            HorizontalDivider(
//                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
//                color = Color(0xFFF0F0F0)
//            )

            Text(
                text = "알림 설정",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF616161),
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )

//            NotificationSwitch(
//                title = "활동 알림",
//                subtitle = "댓글, 좋아요 반응 알림",
//                checked = pushActivity,
//                onCheckedChange = onActivityChange
//            )

            NotificationSwitch(
                title = "혜택 알림",
                subtitle = "이벤트 및 마케팅 정보",
                checked = pushMarketing,
                onCheckedChange = onMarketingChange
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            Text(
                text = "서비스 이용",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF616161),
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Article, null, tint = Color(0xFF666666), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "현재 버전", fontSize = 16.sp, color = Color(0xFF333333), modifier = Modifier.weight(1f))
                Text(text = "v$versionName", fontSize = 14.sp, color = Color(0xFF616161))
            }
            // 문의하기
            MenuItem(
                Icons.Outlined.Help,
                "문의하기",
                onClick = {
                    navController.navigate(Screen.Inquiry.route)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            MenuItem(
                icon = Icons.Outlined.Logout,
                title = "로그아웃",
                iconTint = Color(0xFFE53935),
                titleColor = Color(0xFFE53935),
                onClick = onLogoutClick
            )

            MenuItem(
                icon = Icons.Default.Close,
                title = "회원 탈퇴",
                iconTint = Color(0xFFE53935),
                titleColor = Color(0xFFE53935),
                onClick = onDeleteAccountClick
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
            tint = Color(0xFF757575),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun NotificationSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column (modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF616161))
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1976D2),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}