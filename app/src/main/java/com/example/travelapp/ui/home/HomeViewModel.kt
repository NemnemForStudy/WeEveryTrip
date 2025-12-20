package com.example.travelapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.repository.AuthRepository
import com.example.travelapp.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 게시물 데이터 클래스
@HiltViewModel // 이 클래스를 ViewModel로 인식하고 의존성 주입 관리 할 수 있게 함.
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    // UI에 노출될 검색어 상태(읽기 전용 StateFlow)
    // StateFlow는 현재 상태, 새 상태 업데이트 수신하는 관찰 가능한 상태 홀더 흐름.
    // StateFlow로 상태가 변경될 때마다 화면을 다시 그림
    private val _searchQuery = MutableStateFlow("") // viewModel 내부에서만 값 변경 가능.
    val searchQuery = _searchQuery.asStateFlow() // 읽기 가능한 상태로 노출함. 상태 캡슐화라 함.
    
    // 검색창 표시 여부 관리 상태
    private val _showSearchBar = MutableStateFlow(false)
    val showSearchBar = _showSearchBar.asStateFlow()

    // 검색 결과 목록
    private val _searchResults = MutableStateFlow<List<Post>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _myPosts = MutableStateFlow<List<Post>>(emptyList())
    val myPosts = _myPosts.asStateFlow()

    private val _myPostsLoading = MutableStateFlow(false)
    val myPostsLoading = _myPostsLoading.asStateFlow()

    private val _myPostsError = MutableStateFlow<String?>(null)
    val myPostsError = _myPostsError.asStateFlow()

    init {
        loadMyPosts()
    }

    /**
     * 사용자가 검색창에 텍스트 입력할 때마다 호출되는 함수
     * @param query 사용자가 입력한 새 검색어
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * 검색 아이콘 클릭했을 때 검색창 표시 함수
     */
    fun openSearchBar() {
        _showSearchBar.value = true
    }

    /**
     * 뒤로가기 버튼이나 검색 실행 후 검색창 닫는 함수
     */

    fun closeSearchBar() {
        _showSearchBar.value = false
        _searchResults.value = emptyList() // 검색창 닫을 때 결과도 초기화
    }

    /**
     * 키보드 검색 버튼을 누르거나 검색 아이콘 검색 눌렀을때 검색되게
     * 백엔드 서버에 검색 요청 보내고 결과를 받아옴.
     */

    fun performSearch(query: String) {
        // viewModelScope는 ViewModel이 소멸될 때 함께 취소되는 코루틴 스코프
        // 네트워크 요청과 같은 비동기 작업을 안전하게 처리할 수 있음.
        viewModelScope.launch {
            if(query.isNotBlank()) {
                _isLoading.value = true

                try {
                    // 실제 네트워크 요청 코드 추가함.
                    val result = postRepository.searchPostsByTitle(query.trim())
                    result.onSuccess { posts ->
                        _searchResults.value = posts
                        println("검색 성공: ${posts.size} 개 결과")
                    }
                    result.onFailure { error ->
                        println("검색 실패: ${error.message}")
                        println("검색 실패 스택: ${error.stackTrace}")
                        _searchResults.value = emptyList()
                    }
                } catch (e: Exception) {
                    println("검색 중 오류 발생: ${e.message}")
                    e.printStackTrace()
                    _searchResults.value = emptyList()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun loadMyPosts() {
        viewModelScope.launch {
            _myPostsLoading.value = true
            _myPostsError.value = null

            try {
                val myIdResult = authRepository.getUserId()
                val myId = myIdResult.getOrNull()

                if(myId == null) {
                    _myPosts.value = emptyList()
                    _myPostsError.value = "내 정보(userId)를 가져오지 못했습니다."
                    // 코드를 실행하기 위한 '최소 조건'이 충족되지 않았을 때, 미리 예외 처리를 하고 함수를 종료시키는 것
                    // 코루틴 return에서는 단독으로 return할 수는 없다.
                    // 그래서 현재 진행 중인 이 코루틴 블록만 빠져나가겠다. 라고 선언.
                    return@launch
                }

                val postsResult = postRepository.getAllPosts()
                // 내 아이디와 일치하는 게시물만 화면에 보여주려고 하는거임.
                // posts.filter -> 필터링이고, it.userId == myId는 내 id랑 일치하는지 확인함.
                postsResult.onSuccess { posts ->
                    _myPosts.value = posts.filter { it.userId == myId }
                }.onFailure { e ->
                    _myPosts.value = emptyList()
                    _myPostsError.value = e.message ?: "내 글 불러오기 실패"
                }
            } catch (e: Exception) {
                _myPosts.value = emptyList()
                _myPostsError.value = e.message ?: "내 글 불러오기 실패"
            } finally {
                _myPostsLoading.value = false
            }
        }
    }

    fun fetchMyPosts() {
        loadMyPosts()
    }
}