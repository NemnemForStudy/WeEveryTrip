package com.example.travelapp.ui.Detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postApiService: PostApiService
): ViewModel() {
    // UI가 바라볼 상태 변수들
    // 게시물 데이터
    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

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
}
