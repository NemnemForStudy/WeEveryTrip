package com.example.travelapp.ui.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.model.CreatePostRequest
import com.example.travelapp.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// HiltViewModel - Hilt가 이 ViewModel 생성하고 필요한 의존성 주입할 수 있도록 함.
@HiltViewModel
class WriteViewModel @Inject constructor(
    private val postRepository: PostRepository // 의존성 주입
) : ViewModel() {
    // 게시물 등록 작업의 현재 상태를 나타내는 StateFlow
    // _postCreateionStatus는 ViewModel 내부에서만 값 변경,
    // postCreationStatus는 외부에서 읽기 전용으로 관찰할 수 있도록 노출 함.

    private val _postCreationStatus = MutableStateFlow<PostCreationStatus>(PostCreationStatus.Idle) // 초기 상태 아무것도 하지않음.
    val postCreationStatus: StateFlow<PostCreationStatus> = _postCreationStatus

    // 게시물 생성 요청 처리 함수
    // View(WriteScreen)에서 이 함수를 호출해 게시물 등록 시작
    fun createPost(category: String, title: String, content: String, tags: List<String>, imgUrl: String?= null) {
        // viewModelScope는 ViewModel이 제거될 때 자동으로 취소되는 코루틴 스코프 제공
        viewModelScope.launch {
            _postCreationStatus.value = PostCreationStatus.Loading
            val request = CreatePostRequest(category, title, content, tags, imgUrl)

            // Repository 통해 게시물 생성 API 호출
            postRepository.createPost(request).onSuccess { post ->
                // API 호출 성공 시 상태 Success 변경, 생성 게시물 데이터 전달
                _postCreationStatus.value = PostCreationStatus.Success(post.id)
            }
            . onFailure { throwable ->
                // API 호출 실패 시 Error 상태로 변경하고, 에러 메시지 전달함.
                _postCreationStatus.value = PostCreationStatus.Error(throwable.localizedMessage ?: "알 수 없는 오류 발생")
            }
        }
    }

    // 게시물 생성 상태를 나타내는 sealed 클래스
    // View는 이 상태를 관찰해 UI를 업데이트 할 수 있음.
    sealed class PostCreationStatus {
        object Idle: PostCreationStatus()
        object Loading: PostCreationStatus()
        data class Success(val postId: String): PostCreationStatus()
        data class Error(val message: String): PostCreationStatus()
    }
}