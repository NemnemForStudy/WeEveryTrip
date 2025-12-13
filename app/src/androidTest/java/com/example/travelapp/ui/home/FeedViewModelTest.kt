package com.example.travelapp.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI 상태를 나타내는 Sealed Class (성공/로딩/에러)
sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val posts: List<Post>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _feedUiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val feedUiState: StateFlow<FeedUiState> = _feedUiState.asStateFlow()

    init {
        // 화면 진입 시 자동으로 데이터 로드
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _feedUiState.value = FeedUiState.Loading

            // Repository에 getAllPosts 함수가 필요합니다.
            // 현재 PostRepository에 이 함수가 없다면 추가해야 합니다.
            postRepository.getAllPosts()
                .onSuccess { posts ->
                    _feedUiState.value = FeedUiState.Success(posts)
                }
                .onFailure { exception ->
                    _feedUiState.value = FeedUiState.Error(exception.message ?: "알 수 없는 오류")
                }
        }
    }
}