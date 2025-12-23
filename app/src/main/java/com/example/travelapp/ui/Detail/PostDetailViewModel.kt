package com.example.travelapp.ui.Detail

import android.hardware.camera2.CaptureFailure
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.data.repository.AuthRepository
import com.example.travelapp.data.repository.CommentRepository
import com.example.travelapp.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
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

    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    private val _routePointsByDay = MutableStateFlow<Map<Int, List<RoutePoint>>>(emptyMap())
    val routePointsByDay: StateFlow<Map<Int, List<RoutePoint>>> = _routePointsByDay.asStateFlow()

    private val _currentDayIndex = MutableStateFlow(0)
    val currentDayIndex: StateFlow<Int> = _currentDayIndex.asStateFlow()

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
                // 사진이 있는 Day만 추출
                val fetchPost = postApiService.getPostById(postId)

                val photoDays = fetchPost.imageLocations
                    .mapNotNull { loc ->
                        val lat = loc.latitude
                        val lng = loc.longitude
                        val day = loc.dayNumber ?: 0
                        if(lat != null && lng != null) day to RoutePoint(lat, lng) else null
                    }
                    .groupBy({ it.first }, { it.second })

                // 전체 여행 기간 계산
                val totalDays = run {
                    val start = fetchPost.travelStartDate
                    val end = fetchPost.travelEndDate
                    if(start != null && end != null) {
                        // 날짜 차이 계산
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        try {
                            val startDate = sdf.parse(start)
                            val endDate = sdf.parse(end)
                            if(startDate != null && endDate != null) {
                                ((endDate.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                            } else 1
                        } catch (e: Exception) { 1 }
                    } else 1
                }

                val pointsByDay = (1..totalDays).associateWith { day ->
                    photoDays[day] ?: emptyList()
                }.toSortedMap()

                if(pointsByDay.isNotEmpty()) {
                    _routePointsByDay.value = pointsByDay
                    // pointsByDay의 key 들을 오름차순 정렬
                    val sortedKeys = pointsByDay.keys.sorted()

                    // 사진이 실제로 있는 첫 번째 Day 찾기
                    val firstDayWithPhotos = sortedKeys.firstOrNull() { day ->
                        pointsByDay[day]?.isNotEmpty() == true
                    }

                    // 찾은 Day가 sortedKeys에서 몇 번째 인덱스인지 계산
                    _currentDayIndex.value = if(firstDayWithPhotos != null) {
                        sortedKeys.indexOf(firstDayWithPhotos) // 사진 있는 Day의 인덱스
                    } else {
                        0 // 사진 있는 Day가 없으면 그냥 0
                    }
                } else {
                    _routePointsByDay.value = emptyMap()
                    _currentDayIndex.value = 0
                }


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

    fun updatePost(
        category: String? = null,
        title: String? = null,
        content: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        isDomestic: Boolean? = null
    ) {
        val postId = _currentPostId.value
        if(postId.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true

            val result = postRepository.updatePost(
                postId = postId,
                category = category,
                title = title,
                content = content,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                isDomestic = isDomestic
            )

            if(result.isSuccess) {
                _post.value = result.getOrNull()
            } else {
                Log.e("PostDetailViewModel", "게시물 수정 실패: ${result.exceptionOrNull()?.message}")
                _errorMsg.value = "게시물 수정 실패"
            }

            _isLoading.value = false
        }
    }

    fun fetchRoute(locations: List<RoutePoint>) {
        viewModelScope.launch {
            if(locations.size < 2) {
                _routePoints.value = emptyList()
                return@launch
            }

            Log.d("PostDetail", "fetchRoute: locations=${locations.size}")
            val route = postRepository.getRouteForDay(locations)
            Log.d("PostDetail", "fetchRoute: routeSize=${route?.size}")
            _routePoints.value = route ?: emptyList()
        }
    }

    fun clearRoute() {
        _routePoints.value = emptyList()
    }

    fun deletePost(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val postId = _currentPostId.value
        if(postId.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = postRepository.deletePost(postId)
            _isLoading.value = false

            if(result.isSuccess) {
                onSuccess()
            } else {
                val message = result.exceptionOrNull()?.message ?: "게시물 삭제 실패"
                _errorMsg.value = message
                onFailure(message)
            }
        }
    }

    fun goToPrevDay() {
        val totalDays = _routePointsByDay.value.size
        if(totalDays == 0) return
        _currentDayIndex.value = (_currentDayIndex.value - 1).coerceAtLeast(0)
    }

    fun goToNextDay() {
        val totalDays = _routePointsByDay.value.size
        if(totalDays == 0) return
        _currentDayIndex.value = (_currentDayIndex.value + 1).coerceAtMost(totalDays - 1)
    }

    fun setDayIndex(index: Int) {
        val totalDays = _routePointsByDay.value.size
        if(totalDays == 0) return
        _currentDayIndex.value = index.coerceIn(0, totalDays - 1)
    }
}
