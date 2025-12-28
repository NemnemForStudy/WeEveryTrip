package com.example.travelapp.ui.home

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.travelapp.ui.components.BottomNavigationBar
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.TravelAppTheme

/**
 * 로그인이 성공한 후 보여질 메인 홈 화면
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    // 생명주기에 맞춰 viewModel이 자동으로 관리됨.
    viewModel: HomeViewModel = hiltViewModel()
) {
   // 검색어 저장할 상태 변수
    var searchQuery by remember { mutableStateOf("") }
    val showSearchBar by viewModel.showSearchBar.collectAsState()
    // 소프트웨어 키보드 컨트롤러
    val keyboardController = LocalSoftwareKeyboardController.current
    val myPosts by viewModel.myPosts.collectAsState()
    val myPostsLoading by viewModel.myPostsLoading.collectAsState()
    val myPostsError by viewModel.myPostsError.collectAsState()

    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val shouldRefresh = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("post_deleted", true)
        ?.collectAsState()

    LaunchedEffect(shouldRefresh?.value) {
        if(shouldRefresh?.value == true) {
            viewModel.fetchMyPosts()
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("post_deleted", false)
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
                            text = "모여로그",
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
                    containerColor = MaterialTheme.colorScheme.background
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
                onClick = { navController.navigate(Screen.Write.route) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "글 등록")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        // 텐츠 영역
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            if (showSearchBar) {
                when {
                    isLoading -> {
                        Text(
                            text = "검색 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF616161),
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    searchResults.isEmpty() -> {
                        Text(
                            text = "검색 결과가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF616161),
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(searchResults.size) { index ->
                                val post = searchResults[index]
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    PostCard(
                                        post = post,
                                        onClick = { navController.navigate("detail/${post.id}") }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when {
                    myPostsLoading -> {
                        Text(
                            text = "내 글 불러오는 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF616161),
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    myPostsError != null -> {
                        Text(
                            text = myPostsError ?: "오류가 발생했습니다.",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    myPosts.isEmpty() -> {
                        Text(
                            text = "작성한 글이 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF616161),
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(myPosts.size) { index ->
                                val post = myPosts[index]
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    PostCard(
                                        post = post,
                                        onClick = { navController.navigate("detail/${post.id}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TravelAppTheme {
        HomeScreen(navController = rememberNavController())
    }
}