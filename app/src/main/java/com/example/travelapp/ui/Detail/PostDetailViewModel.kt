package com.example.travelapp.ui.Detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.data.repository.AuthRepository
import com.example.travelapp.data.repository.CommentRepository
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
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
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

    private val _commentContent = MutableStateFlow("")
    val commentContent = _commentContent.asStateFlow()

    private val _currentPostId = MutableStateFlow("")
    val currentPostId = _currentPostId.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId = _currentUserId.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()

    init {
        loadCurrentUserId()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            val result = authRepository.getUserId()
            if (result.isSuccess) {
                _currentUserId.value = result.getOrNull().orEmpty()
            }
        }
    }
    // 데이터 가져오는 함수
    fun fetchPostDetail(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null

            _currentPostId.value = postId
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
    fun loadComments(postId: String) {
        _currentPostId.value = postId
        viewModelScope.launch {
            val result = commentRepository.getComments(postId)
            if(result.isSuccess) {
                val newList = result.getOrNull() ?: emptyList()
                if (newList.isEmpty() && _comments.value.isNotEmpty()) {
                    return@launch
                }
                _comments.value = newList
            } else {
                Log.e("PostDetailViewModel", "댓글 로드 실패")
            }
        }
    }

    fun createComment(postId: String) {
        val content = _commentContent.value
        if(content.isBlank()) return

        viewModelScope.launch {
            val result = commentRepository.createComment(postId, content)

            if(result.isSuccess) {
                _commentContent.value = ""

//                val newComment = result.getOrNull()
//
//                if(newComment != null) {
//                    val currentList = _comments.value.toMutableList()
//
//                    val displayComment = if(newComment.nickname.isNullOrBlank() || newComment.nickname == "알 수 없음") {
//                        newComment.copy(nickname = "내 닉네임")
//                    } else {
//                        newComment
//                    }
//
//                    currentList.add(0, displayComment)
//                    _comments.value = currentList
//                }

                loadComments(postId)
            } else {
                Log.e("PostDetailViewModel", "댓글 작성 실패: ${result.exceptionOrNull()?.message}")
                _errorMsg.value = "댓글 작성 실패"
            }
        }
    }

    fun updateComment(commentId: String, newContent: String) {
        val postId = _currentPostId.value
        if(postId.isBlank() || newContent.isBlank()) return

        viewModelScope.launch {
            val result = commentRepository.updateComment(commentId, newContent)

            if(result.isSuccess) {
                loadComments(postId)
            } else {
                Log.e("PostDetailViewModel", "댓글 수정 실패: ${result.exceptionOrNull()?.message}")
                _errorMsg.value = "댓글 수정 실패"
            }
        }
    }

    fun deleteComment(commentId: String) {
        val postId = _currentPostId.value
        if(postId.isBlank()) return

        viewModelScope.launch {
            val result = commentRepository.deleteComment(commentId)

            if(result.isSuccess) {
                loadComments(postId)
            } else {
                Log.e("PostDetailViewModel", "댓글 삭제 실패: ${result.exceptionOrNull()?.message}")
                _errorMsg.value = "댓글 삭제 실패"
            }
        }
    }

    fun updateCommentInput(content: String) {
        _commentContent.value = content
    }
}
