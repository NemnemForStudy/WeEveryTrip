package com.example.travelapp.ui.Detail // 패키지명 확인

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.util.UtilTime
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.Align
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import kotlin.math.exp

// 색상 상수
val PrimaryBlue = Color(0xFF4A90E2)
val LightGrayBg = Color(0xFFF5F7FA)
val TextGray = Color(0xFF888888)
val TextDark = Color(0xFF222222)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val post by viewModel.post.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val isLiked by viewModel.isLiked.collectAsStateWithLifecycle()
    val likeCount by viewModel.likeCount.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentContent by viewModel.commentContent.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    // 화면 진입 시 딱 한 번 실행 (데이터 요청)
    LaunchedEffect(postId) {
        viewModel.fetchPostDetail(postId)
        viewModel.loadLikeData(postId)
        viewModel.loadComments(postId)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            errorMsg != null -> {
                Text(
                    text = errorMsg ?: "알 수 없는 오류",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            post != null -> {
                val isMyPost = post!!.userId == currentUserId

                PostDetailContent(
                    post = post!!,
                    isLiked = isLiked,
                    likeCount = likeCount,
                    comments = comments,
                    commentInput = commentContent,
                    currentUserId = currentUserId,
                    onLikeToggle = { viewModel.toggleLike(postId) },
                    onCommentChange = { viewModel.updateCommentInput(it) },
                    onCommentSend = { viewModel.createComment(postId) },
                    onCommentEdit = { comment, newContent ->
                        viewModel.updateComment(comment.commentId, newContent)
                    },
                    onCommentDelete = { commentId ->
                        viewModel.deleteComment(commentId)
                    },
                    onBackClick = {
                        navController.navigate("feed") {
                            popUpTo("feed") { inclusive = true }
                        }
                    },
                    onPostEdit = {
                        navController.navigate("edit/${postId}")
                    },
                    onPostDelete = {
                        // TODO: 삭제 확인 다이얼로그
                    },
                    isMyPost = isMyPost
                )
            }
            // post 가 null, 에러 없고, 로딩도 아닐 떄 대비한 else
            else -> {
                // 데이터 로드에 실패했지만 에러 메시지가 없는 경우
                Text(
                    text = "게시물을 찾을 수 없습니다.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalNaverMapApi::class)
@Composable
fun PostDetailContent(
    post: Post,
    isLiked: Boolean,
    likeCount: Int,
    comments: List<Comment>,
    commentInput: String,
    currentUserId: String,
    onLikeToggle: () -> Unit,
    onCommentChange: (String) -> Unit,
    onCommentSend: () -> Unit,
    onCommentEdit: (Comment, String) -> Unit,
    onCommentDelete: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onPostEdit: () -> Unit = {},
    onPostDelete: () -> Unit = {},
    isMyPost: Boolean = false
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }

    Scaffold(
        topBar = {
            // Material3 최신 버전에서는 SmallTopAppBar 대신 TopAppBar 사용 권장
            TopAppBar(
                title = {},
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.8f),
                        shadowElevation = 4.dp
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* 공유 기능 */ }) {
                        Icon(Icons.Default.Share, contentDescription = "공유", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            CommentInputBar(
                value = commentInput,
                onValueChange = onCommentChange,
                onSendClick = onCommentSend,
                isLiked = isLiked,
                likeCount = likeCount,
                onLikeClick = onLikeToggle
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { PostImageHeader(post) }

            item {
                PostBodySection(
                    post = post,
                    likeCount = likeCount,
                    commentCount = comments.size,
                    isMyPost = isMyPost,
                    onEditClick = onPostEdit,
                    onDeleteClick = onPostDelete
                )
            }

            item {
                HorizontalDivider(thickness = 8.dp, color = LightGrayBg)
            }

            item {
                Text(
                    text = "댓글 ${comments.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
                )
            }

            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("첫 번째 댓글을 남겨보세요!", color = Color.Gray)
                    }
                }
            } else {
                items(comments) { comment ->
                    CommentItem(
                        comment = comment,
                        currentUserId = currentUserId,
                        onEditClick = { selectedComment ->
                            commentToEdit = selectedComment
                            showEditDialog = true
                        },
                        onDeleteClick = { commentToDelete ->
                            onCommentDelete(commentToDelete.commentId)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showEditDialog && commentToEdit != null) {
        EditCommentDialog(
            comment = commentToEdit!!,
            onDismiss = {
                showEditDialog = false
                commentToEdit = null
            },
            onConfirm = { newContent ->
                onCommentEdit(commentToEdit!!, newContent)
                showEditDialog = false
                commentToEdit = null
            }
        )
    }
}

/**
 * 하단 고정 바
 */
@Composable
fun PostDetailBottomBar (
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    commentCount: Int
) {
    Surface(
        shadowElevation = 16.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if(isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = if(isLiked) Color.Red else Color.Gray
                    )
                }

                Text(text = "${likeCount}", fontSize = 14.sp)

                Spacer(modifier = Modifier.width(16.dp))

                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "댓글", tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$commentCount", fontSize = 14.sp)
            }

            Button(
                onClick = {/*액션*/},
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("여행 일정 담기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostImageHeader(post: Post) {
    // 대형 이미지 헤더
    val imageList = (post.images ?: emptyList()).ifEmpty { listOfNotNull(post.imgUrl) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { if(imageList.isNotEmpty()) imageList.size else 1 } // 큰 수
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .background(Color.LightGray)
    ) {
        if (imageList.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val fullUrl = BuildConfig.BASE_URL + imageList[page]
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fullUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "게시물 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // 페이지 인디케이터
            if (imageList.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(imageList.size) { index ->
                        val isSelected = pagerState.currentPage === index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White
                                    else Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Text("이미지가 없는 게시물입니다.", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun PostBodySection(
    post: Post,
    likeCount: Int = 0,
    commentCount: Int = 0,
    isMyPost: Boolean = false,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-20).dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        // 카테고리 태그
        Surface(
            color = PrimaryBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = post.category ?: "카테고리 없음",
                color = PrimaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFFE0E0E0)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(post.nickname.take(1), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = post.nickname, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Text(
                        text = UtilTime.formatRelativeTime(post.created_at),
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            // 본인 게시물일 때만 점 세개 표시
            if (isMyPost) {
                Box {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = {
                                isMenuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", color = Color.Red) },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color(0xFFE0E0E0))

        Text(
            text = post.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = post.content,
            fontSize = 16.sp,
            color = Color(0xFF444444),
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text("위치 정보", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (post.latitude != null && post.longitude != null) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition(LatLng(post.latitude!!, post.longitude!!), 14.0)
            }
            NaverMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                cameraPositionState = cameraPositionState
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text("위치 정보가 없습니다.", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if(post.tags != null && post.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                post.tags.forEach { tag ->
                    Text(
                        text = "#$tag",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String,
    onEditClick: (Comment) -> Unit,
    onDeleteClick: (Comment) -> Unit
) {
    // 메뉴가 펼쳐졌는지 여부 저장
    var isMenuExpanded by remember { mutableStateOf(false) }

    // 내 댓글인지 확인 (내 댓글일때만 점 3개가 보임)
    // 서버에서 오는 ID 타입
    val isMyContent = comment.userId.toString() == currentUserId

    Column{
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon (
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Color.LightGray
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column (
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text (
                        text = comment.nickname ?: "알 수 없음",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 날짜
                    Text(
                        text = comment.createdAt.take(10),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.content,
                    fontSize = 15.sp,
                    color = TextDark,
                    lineHeight = 20.sp
                )
            }

            if(isMyContent) {
                Box {
                    IconButton (
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = (-4).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.Gray
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = {
                                isMenuExpanded = false
                                onEditClick(comment)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("삭제", color = Color.Red) },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteClick(comment)
                            }
                        )
                    }
                }
            }
        }
        Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(top = 12.dp))
    }
}

/**
 * 댓글 입력창 + 좋아요 버튼(하단 고정)
 */
@Composable
fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit
) {
    Surface(
        shadowElevation = 16.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좋아요 버튼 (왼쪽)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                IconButton(onClick = onLikeClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = if (isLiked) Color.Red else Color.Gray
                    )
                }
                Text(text = "$likeCount", fontSize = 12.sp, color = Color.Gray)
            }

            // 입력창 (중간)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("댓글을 입력하세요...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = PrimaryBlue
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 전송 버튼 (오른쪽)
            IconButton(
                onClick = onSendClick,
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (value.isNotBlank()) PrimaryBlue else Color.LightGray,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 프리뷰
@Preview
@Composable
fun PostDetailScreenPreview() {
    val dummyPost = Post(
        id = "1",
        category = "국내여행",
        title = "강릉 안목해변 카페거리 정복기",
        content = "내용 예시입니다.",
        nickname = "바다조아",
        created_at = "2025-12-06",
        tags = listOf("강릉", "카페"),
        images = emptyList(),
        imgUrl = null
    )
//    PostDetailContent(post = dummyPost)
}