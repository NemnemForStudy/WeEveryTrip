package com.example.travelapp.ui.write

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.model.CreatePostRequest
import com.example.travelapp.data.repository.PostRepository
import com.kakao.sdk.template.model.Content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude.asStateFlow()

    private val _postCreationStatus = MutableStateFlow<PostCreationStatus>(PostCreationStatus.Idle) // 초기 상태 아무것도 하지않음.
    val postCreationStatus: StateFlow<PostCreationStatus> = _postCreationStatus.asStateFlow()

    /**
     * 사용자가 지도에서 직접 마커를 움직여 위치를 변경했을 때 호출됩니다.
     */
    fun updateLocation(lat: Double, lon: Double) {
        _latitude.value = lat
        _longitude.value = lon
    }

    /**
     * [핵심] 게시글 생성 요청
     * WriteScreen에서 입력한 모든 데이터를 파라미터로 받습니다.
     * (ViewModel 내부의 _title 등을 참조하지 않고, 받은 값을 그대로 사용합니다.)
     */
    fun createPost(
        category: String,
        title: String,
        content: String,
        tags: List<String>,
        imgUris: List<Uri>
    ) {
        if(title.isBlank() || content.isBlank()) {
            _postCreationStatus.value = PostCreationStatus.Error("제목과 내용을 입력해주세요.")
            return
        }

        // viewModelScope는 ViewModel이 제거될 때 자동으로 취소되는 코루틴 스코프 제공
        viewModelScope.launch {
            _postCreationStatus.value = PostCreationStatus.Loading

            // 1. ViewModel이 기억하고 있는 현재 위치 정보를 가져옵니다.
            val currentLat = _latitude.value
            val currentLon = _longitude.value

            try {
                // TODO: 리포지토리의 createPost 함수에 lat, lon 파라미터를 추가해서 넘겨야 합니다.
                // postRepository.createPost(category, title, content, tags, imgUris, currentLat, currentLon)

                kotlinx.coroutines.delay(1000) // 네트워크 딜레이 시뮬레이션
                _postCreationStatus.value = PostCreationStatus.Success("temp_post_id_123")
            } catch (e: Exception) {
                _postCreationStatus.value = PostCreationStatus.Error(e.localizedMessage ?: "예외 발생")
            }
        }
    }

    // 게시물 작성 완료 또는 취소 후 상태 초기화
    fun resetStatus() {
        _postCreationStatus.value = PostCreationStatus.Idle
        _latitude.value = null
        _longitude.value = null
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