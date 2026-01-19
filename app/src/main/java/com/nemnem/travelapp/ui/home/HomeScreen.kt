package com.nemnem.travelapp.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.nemnem.travelapp.ui.components.BottomNavigationBar
import com.nemnem.travelapp.ui.components.EmptyTravelState
import com.nemnem.travelapp.ui.navigation.Screen
import com.nemnem.travelapp.ui.theme.Beige
import com.nemnem.travelapp.ui.theme.PointRed
import com.nemnem.travelapp.ui.theme.TextSub
import com.nemnem.travelapp.ui.theme.TravelAppTheme

/**
 * 로그인이 성공한 후 보여질 메인 홈 화면
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    // 생명주기에 맞춰 viewModel이 자동으로 관리됨.
    viewModel: HomeViewModel = hiltViewModel(),
    isLoggedIn: Boolean
) {
   // 검색어 저장할 상태 변수
    var searchQuery by remember { mutableStateOf("") }
    val showSearchBar by viewModel.showSearchBar.collectAsState()
    // 소프트웨어 키보드 컨트롤러
    val keyboardController = LocalSoftwareKeyboardController.current
    val myPosts by viewModel.myPosts.collectAsState()
    val myPostsLoading by viewModel.myPostsLoading.collectAsState()
    val myPostsError by viewModel.myPostsError.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }

    val deleteSignal by navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow("post_deleted", false)!!.collectAsState()

    val createSignal by navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow("post_created", false)!!.collectAsState()

    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val shouldRefresh = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("post_deleted", true)
        ?.collectAsState()

    LaunchedEffect(deleteSignal, createSignal) {
        if (deleteSignal || createSignal) {
            Log.d("HomeScreen", "새로고침 신호 감지: 리스트를 다시 불러옵니다.")
            viewModel.fetchMyPosts()

            // 신호를 처리했으므로 다시 false로 초기화 (중요)
            navController.currentBackStackEntry?.savedStateHandle?.set("post_deleted", false)
            navController.currentBackStackEntry?.savedStateHandle?.set("post_created", false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { newValue ->
                                Log.d("HomeScreen", "입력됨: $newValue")
                                searchQuery = newValue
                            },
                            placeholder = { Text("검색어를 입력하세요") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    Log.d("HomeScreen", "onSearch triggered! 검색어: $searchQuery")
                                    keyboardController?.hide()
                                    viewModel.performSearch(searchQuery)
                                }
                            ),
                            leadingIcon = {
                                IconButton(onClick = {
                                    viewModel.closeSearchBar()
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.ArrowBack, "뒤로 가기", tint = Color(0xFF212121))
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = "ModuTrip",
                            // ✅ headlineMedium에서 headlineLarge(우리가 정한 ExtraBold)로 변경
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color(0xFF111111)
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                actions = {
                    // Search button
                    IconButton(onClick = { viewModel.openSearchBar() }) {
                        Icon(Icons.Filled.Search, contentDescription = "검색", tint = Color(0xFF212121))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Beige
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = Screen.Home.route
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if(isLoggedIn) {
                        navController.navigate(Screen.Write.route)
                    } else {
                        showLoginDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = PointRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "글 등록")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        // 콘텐츠 영역
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Beige)
        ) {

            if(!isLoggedIn) {
                EmptyTravelState(
                    description = "로그인을 하시면 사진 속 위치로\n나만의 여행 기록을 만들 수 있어요!",
                    buttonText = "여행 기록 만들러 가기",
                    onButtonClick = {
                        // 로그인 화면으로 이동
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            } else {
                when {
                    myPostsLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PointRed)
                    }

                    myPostsError != null -> {
                        EmptyTravelState(
                            title = "앗! 문제가 생겼어요",
                            description = "데이터를 불러오는 중에 오류가 발생했습니다.\n잠시 후 다시 시도해 주세요.",
                            buttonText = "다시 시도",
                            onButtonClick = { viewModel.fetchMyPosts() }
                        )
                    }

                    myPosts.isEmpty() -> {
                        EmptyTravelState(
                            title = "아직 기록이 없어요",
                            description = "첫 번째 여행 사진을 올려서\n나만의 지도를 채워보세요!",
                            buttonText = "첫 기록 남기기",
                            onButtonClick = { navController.navigate(Screen.Write.route) }
                        )
                    }

                    else -> {
                        val displayPosts = if (showSearchBar && searchQuery.isNotBlank()) {
                            searchResults
                        } else {
                            myPosts
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                        ) {
                            items(displayPosts.size) { index ->
                                PostCard(
                                    post = displayPosts[index], // displayPosts로 변경
                                    onClick = { navController.navigate("detail/${displayPosts[index].id}") }
                                )
                            }
                        }
                    }
                }
            }

            if(showLoginDialog) {
                AlertDialog(
                    onDismissRequest = { showLoginDialog = false },
                    title = { Text("로그인 안내") },
                    text = { Text("여행을 기록하려면 로그인이 필요해요.\n로그인 화면으로 이동할까요?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showLoginDialog = false
                            navController.navigate(Screen.Login.route)
                        }) {
                            Text("네, 이동할게요", color = PointRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLoginDialog = false }) {
                            Text("아니요", color = TextSub)
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TravelAppTheme {
        HomeScreen(
            navController = rememberNavController(),
            isLoggedIn = true
        )
    }
}