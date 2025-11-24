package com.example.travelapp.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation.Companion.keyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.ui.navigation.Screen
import com.example.travelapp.ui.theme.TravelAppTheme

/**
 * 로그인이 성공한 후 보여질 메인 홈 화면
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    // 검색어 저장할 상태 변수
    var searchQuery by remember { mutableStateOf("") }
    // 소프트웨어 키보드 컨트롤러
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSearchBar by remember { mutableStateOf(false) }

    val performSearch = { query: String ->
        if(query.isNotBlank()) {
            // 실제 검색 로직 구현
            // 네트워크 요청, 로컬 데이터 검색
            Log.d("Search", "검색어: $query")

            // 검색 후 검색바 닫기
            showSearchBar = false
            keyboardController?.hide()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if(showSearchBar) {
                        // 검색 입력 필드
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("검색어를 입력하세요") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            // 엔터 키를 검색 버튼으로 변경
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            // 검색 버튼 클릭 시 동작
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    // 검색 로직 추가
                                    performSearch(searchQuery)
                                }
                            ),
                            leadingIcon = {
                                // 뒤로 가기 버튼
                                IconButton(onClick = { showSearchBar = false }) {
                                    Icon(Icons.Default.ArrowBack, "뒤로 가기")
                                }
                            },
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = MaterialTheme.colorScheme.surface,
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
                    IconButton(onClick = { showSearchBar = true }) {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "자유 게시판",
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "자유 게시판", fontSize = 10.sp)
                }

                // 여행 카테고리
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.List,
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
        // 메인 콘텐츠 영역
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "메인 콘텐츠 영역", style = MaterialTheme.typography.bodyLarge)
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