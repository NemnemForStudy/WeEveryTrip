package com.example.travelapp.ui.Detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postApiService: PostApiService,
    private val postRepository: PostRepository
): ViewModel() {
    // UI가 바라볼 상태 변수들
    // 게시물 데이터
    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked = _isLiked.asStateFlow()

    private val _likeCount = MutableStateFlow(0)
    val likeCount = _likeCount.asStateFlow()

    // 데이터 가져오는 함수
    fun fetchPostDetail(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null

            try {
                val fetchPost = postApiService.getPostById(postId)
                _post.value = fetchPost
            } catch (e: Exception) {
                _errorMsg.value = "게시물을 불러오지 못했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLikeData(postId: String) {
        viewModelScope.launch {
            val likedResult = async { postRepository.isPostLiked(postId) }
            val likeCount = async { postRepository.getLikeCount(postId) }

            val isLiked = likedResult.await().getOrNull() ?: false // Result에서 값 꺼냄.
            val count = likeCount.await().getOrNull() ?: 0

            _isLiked.value = isLiked
            _likeCount.value = count
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val isCurrentlyLiked = _isLiked.value
            val currentCount = _likeCount.value

            // 업데이트. UI 상태를 즉시 변경
            // 현재 상태 반대값 토글
            _isLiked.value = !isCurrentlyLiked
            _likeCount.value = if (isCurrentlyLiked) currentCount - 1 else currentCount + 1

            // 서버 요청
            val result = postRepository.toggleLike(postId, isCurrentlyLiked)

            if(result.isFailure) {
                _isLiked.value = isCurrentlyLiked
                _likeCount.value = currentCount

                // 사용자에게 오류 메시지 표시
                val errorMessage = result.exceptionOrNull()?.message ?: "서버 연결에 실패"
                _errorMsg.value = errorMessage
            }
        }
    }
}
