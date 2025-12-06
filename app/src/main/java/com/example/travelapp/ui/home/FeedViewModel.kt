package com.example.travelapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 게시판(피드) 화면의 ViewModel
 *
 * 책임:
 * - 게시물 목록 관리
 * - 카테고리 필터링
 * - 검색 기능
 * - 무한 스크롤 페이지네이션
 */

@HiltViewModel
open class FeedViewModel @Inject constructor(
    protected val postRepository: PostRepository
) : ViewModel() {
    // 게시물 목록(읽기 전용)
    protected val _post = MutableStateFlow<List<Post>>(emptyList())
    val post = _post.asStateFlow()

    // 선택된 카테고리
    protected val _selectedCategory = MutableStateFlow("전체")
    val selectedCategory = _selectedCategory.asStateFlow()

    // 로딩 상태
    protected val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 에러 메시지
    protected val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    // 페이지 번호(무한 스크롤용)
    protected val _currentPage = MutableStateFlow(1)

    // 사용 가능 카테고리 목록
    val categories = listOf("전체", "여행 후기", "여행 팁", "질문", "추천 장소")

    init {
        // 앱 시작 시 게시물 로드
        loadPosts()
    }

    /**
     * 게시물 목록을 서버에서 로드하는 함수
     *
     * 로직:
     * 1. 로딩 상태를 true로 설정
     * 2. Repository를 통해 API 호출
     * 3. 성공 시 posts 상태 업데이트
     * 4. 실패 시 에러 메시지 표시
     * 5. 로딩 상태를 false로 설정
     */
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null

            try {
                val result = postRepository.getAllPosts()

                result.onSuccess { posts ->
                    _post.value = posts
                }.onFailure { exception ->
                    _errorMsg.value = "불러오기 실패: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMsg.value = "게시물 로드 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 카테고리 변경 함수
     * @param category 선택한 카테고리
     */
    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _currentPage.value = 1 // 카테고리 변경 시 페이지 초기화
        loadPosts()
    }

    /**
     * 무한 스크롤 시 다음 페이지 로드
     */
    fun loadMorePosts() {
        viewModelScope.launch {
            _currentPage.value += 1
            // 다음 페이지 데이터 로드 로직
        }
    }
}