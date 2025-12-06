package com.example.travelapp.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.data.model.Post
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if(showSearchBar) {
                        // 검색 입력 필드
                        TextField(
                            value = searchQuery,
                            onValueChange = { newValue ->
                                Log.d("HomeScreen", "입력됨: $newValue")
                                searchQuery = newValue
                            },
                            placeholder = { Text("검색어를 입력하세요") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            // 엔터 키를 검색 버튼으로 변경
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            // 검색 버튼 클릭 시 동작
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    Log.d("HomeScreen", "onSearch triggered! 검색어: $searchQuery")
                                    keyboardController?.hide()
                                    // 검색 실행도 ViewModel에 위임합니다.
                                    viewModel.performSearch(searchQuery)
                                }
                            ),
                            leadingIcon = {
                                // 뒤로 가기 버튼
                                IconButton(onClick = {
                                    viewModel.closeSearchBar()
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.ArrowBack, "뒤로 가기")
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
                        // 기본 타이틀
                        Text(
                            text = "모여로그",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                actions = {
                    if(!showSearchBar) {
                        // Write button
                        IconButton(onClick = { navController.navigate(Screen.Write.route) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "글쓰기")
                        }
                    }

                    // Search button
                    IconButton(onClick = { viewModel.openSearchBar() }) {
                        Icon(Icons.Filled.Search, contentDescription = "검색")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp)
                    .background(MaterialTheme.colorScheme.background) // 베이지
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
             ) {
                // 메인 화면
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "메인 화면",
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "메인 화면", fontSize = 10.sp)
                }

                // 자유 게시판
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            navController.navigate(Screen.Feed.route)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "자유 게시판",
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "자유 게시판", fontSize = 10.sp)
                }

                // 여행 카테고리
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AirplanemodeActive,
                        contentDescription = "여행 카테고리",
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "여행 카테고리", fontSize = 10.sp)
                }

                // My Page
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "My Page",
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "My Page", fontSize = 10.sp)
                }
            }
        }
    ) { paddingValues ->
        // 텐츠 영역
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if(showSearchBar) {
                val searchResults by viewModel.searchResults.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()

                if(isLoading) {
                    // 로딩 중
                    Text(
                        text = "검색 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = "검색 결과가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // 검색 결과가 있을때 리스트로 표시
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchResults.size) { index ->
                            val post = searchResults[index]
                            PostItem(post = post)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = post.content,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = post.nickname,
                style = MaterialTheme.typography.labelSmall
            )

            Text(
                text = post.created_at,
                style = MaterialTheme.typography.labelSmall
            )
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