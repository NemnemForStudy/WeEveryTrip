package com.example.travelapp.ui.write

import android.content.Context
import android.icu.util.Calendar
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.repository.PostRepository
import com.example.travelapp.util.ExifUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // 여행 시작일 / 종료일 상태
    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    // 여행 기간 내 일별 날짜 목록
    private val _tripDays = MutableStateFlow<List<Long>>(emptyList())
    val tripDays: StateFlow<List<Long>> = _tripDays.asStateFlow()

    // 날짜별로 그룹핑되고 시간순으로 정렬된 이미지 맵
    // Key: 날짜(자정 기준 Long), Value: 정렬된 이미지 Uri 리스트
    private val _groupedImages = MutableStateFlow<Map<Long, List<Uri>>>(emptyMap())
    val groupedImages: StateFlow<Map<Long, List<Uri>>> = _groupedImages.asStateFlow()

    private val _postCreationStatus = MutableStateFlow<PostCreationStatus>(PostCreationStatus.Idle) // 초기 상태 아무것도 하지않음.
    val postCreationStatus: StateFlow<PostCreationStatus> = _postCreationStatus.asStateFlow()

    /**
     * 사용자가 지도에서 직접 마커를 움직여 위치를 변경했을 때 호출됩니다.
     */
    fun updateLocation(lat: Double?, lon: Double?) {
        _latitude.value = lat
        _longitude.value = lon
    }

    fun updateDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    /**
     * 선택된 사진들을 날짜별로 묶고, 시간순으로 정렬합니다.
     * WriteScreen에서 갤러리 선택 직후 호출해주세요.
     */
    fun processSeletedImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            // EXIF 추출은 IO 작업이므로 백그라운드 스레드에서 실행
            val groupedAndSorted = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    // 각 사진 촬영 시간 추출(없으면 0)
                    val time = ExifUtils.extractDate(context, uri) ?: 0L
                    Pair(uri, time)
                }.groupBy { (_, time) ->
                    // 날짜별로 그룹핑 키 생성
                    getDayStartMillis(time)
                }.mapValues { (_, list) ->
                    // 각 그룹 내부에서 시간순 정렬
                    list.sortedBy { it.second }  // Pair 두 번째 값으로 정렬
                        .map { it.first }       // 정렬 후 Uri만 남김
                }.toSortedMap() // 날짜 키 자체도 오름차순
            }

            // 결과 업데이트
            _groupedImages.value = groupedAndSorted
        }
    }

    /**
     * Long 타입의 시작일과 종료일 사이의 모든 날짜(00:00:00)를 List<Long>으로 반환합니다.
     * @param startMillis 시작일 밀리초
     * @param endMillis 종료일 밀리초
     */
    private fun generateDaysBetween(startMillis: Long, endMillis: Long): List<Long> {
        val days = mutableListOf<Long>()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startMillis
            // 시 분 초를 0으로 초기화 해 정확히 하루의 시작 지점을 만듦
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 시작일이 종료일보다 작거나 같을때까지 반복
        while(!calendar.after(endCalendar)) {
            days.add(calendar.timeInMillis)
            calendar.add(Calendar.DATE, 1) // 하루 추가
        }
        return days
    }

    /**
     *  주어진 밀리초(사진 촬영 일시)를 해당 날짜의 자정(00:00:00) 밀리초로 변환합니다.
     *
     */
    private fun getDayStartMillis(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
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

            // 서버로 전송할때는 정렬된 _groupedImages.value 활용할 수 있다.
            // 지금은 UI에서 넘겨준 imgUris 그대로 사용.
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
        _startDate.value = null
        _endDate.value = null
        _groupedImages.value = emptyMap() // 이미지 그룹핑 상태도 초기화
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