package com.example.travelapp.ui.write

import android.content.Context
import android.icu.util.Calendar
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.data.model.RouteRequest
import com.example.travelapp.data.repository.PostRepository
import com.example.travelapp.util.ExifUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject


data class PostImage(
    val id: String = UUID.randomUUID().toString(), // 고유 ID
    val uri: Uri,
    val timestamp: Long,
    val dayNumber: Int
)
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

    // 이미지 그룹핑 타입 변경
    // 날짜별로 그룹핑되고 시간순으로 정렬된 이미지 맵
    // 변경: Map<Int, List<Uri>> (Day 1, Day 2 같은 '일차' 기준)
    // Key가 1이면 Day 1, 2면 Day 2를 의미함.
    private val _groupedImages = MutableStateFlow<Map<Int, List<PostImage>>>(emptyMap())
    val groupedImages: StateFlow<Map<Int, List<PostImage>>> = _groupedImages.asStateFlow()

    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    private val _postCreationStatus = MutableStateFlow<PostCreationStatus>(PostCreationStatus.Idle) // 초기 상태 아무것도 하지않음.
    val postCreationStatus: StateFlow<PostCreationStatus> = _postCreationStatus.asStateFlow()

    // 초기화 블록: 날짜가 변경되면 tripDays 자동 계산
    init {
        // combine은 startDate, endDate 둘 중 하나라도 바뀌면 실행됨
        _startDate.combine(_endDate) { start, end ->
            if(start != null && end != null) {
                generateDaysBetween(start, end)
            } else {
                emptyList()
            }
        }.onEach { days -> 
            _tripDays.value = days
        }.launchIn(viewModelScope) // 생명주기에 맞춰 실행
    }
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
     * 사진 선택하면 'Day N' 기준으로 자동 분류
     */
    fun processSelectedImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            // 현재 설정된 여행 날짜 리스트 가져오기
            val currentTripDays = _tripDays.value

            // EXIF 추출은 IO 작업이므로 백그라운드 스레드에서 실행
            val processedImages = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    // 각 사진 촬영 시간 추출(없으면 0)
                    val time = ExifUtils.extractDate(context, uri) ?: 0L
                    val dayStartMillis = getDayStartMillis(time)

                    // 사진 찍은 날짜가 여행 기간 중 몇 번째 날인지 찾기(없으면 -1)
                    val index = currentTripDays.indexOf(dayStartMillis)
                    val dayNumber = if(index != -1) index + 1 else 0 // 0은 "날짜 미정" 혹은 "범위 밖"

                    PostImage(
                        uri = uri,
                        timestamp = time,
                        dayNumber = dayNumber
                    )
                }
                .filter { it.dayNumber > 0 } // 여행 기간에 포함된 사진만 필터링
                .sortedBy { it.timestamp } // 날짜+시간 순 정렬
            }
            // Map으로 그룹핑
            val newGrouped = processedImages.groupBy { it.dayNumber }

            // 기존 데이터 유지하면서 병합하거나 새로 덮어쓰기
            _groupedImages.value = newGrouped

            // 첫 번째 사진 위치 정보로 업데이트
            if(processedImages.isNotEmpty()) {
                val firstUri = processedImages.first().uri
                val location = ExifUtils.extractLocation(context, firstUri)
                if(location != null) {
                    updateLocation(location.first, location.second)
                }
            }
        }
    }

    // 순서 변경 로직(Swap)
    // dayNumber에 해당하는 이미지 리스트에서 fromIndex위치 이미지를 toIndex 위치로 이동시키고 State 갱신.
    /**
     * 흐름
     * State(Map) -> MutableMap -> MutableList -> MutableList -> MutableMap -> UI업데이트
     */
    fun swapImages(dayNumber: Int, fromIndex: Int, toIndex: Int) {
        // 불변 Map을 그대로 수정하면 Compost가 감지 못함. 반드시 .toMutableMap()으로 새 인스턴스 생성.
        val currentMap = _groupedImages.value.toMutableMap()
        // dayNumber 값가져오고 없으면 null이니 return 해준다.
        // toMutableList() 쓰는 이유 -> 순서 변경하려면 MutableList가 필요함. State 내부 값 직접 건드리지 말고 복사해 수정.
        val list = currentMap[dayNumber]?.toMutableList() ?: return

        // 인덱스 범위 치크. indices = 0 until list.size임. 반드시 필요한 방어 코드임.
        if(fromIndex in list.indices && toIndex in list.indices) {
            // fromIndex에 있는 이미지 꺼냄.
            val item = list.removeAt(fromIndex)
            // 꺼낸 이미지 toIndex 위치에 삽입.
            list.add(toIndex, item)
            currentMap[dayNumber] = list
            // State에 새로운 Map 인스턴스 할당. Compose가 상태 변경을 감지해서 UI 재구성 함.
            _groupedImages.value = currentMap
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
                val result = postRepository.createPost(
                    category = category,
                    title = title,
                    content = content,
                    tags = tags,
                    imageUris = imgUris,
                    latitude = currentLat,
                    longitude = currentLon,
                    isDomestic = true
                )

                result.onSuccess {
                    _postCreationStatus.value = PostCreationStatus.Success(it.id)
                }.onFailure { e ->
                    _postCreationStatus.value = PostCreationStatus.Error(e.message ?: "등록 실패")
                }
            } catch (e: Exception) {
                _postCreationStatus.value = PostCreationStatus.Error(e.localizedMessage ?: "예외 발생")
                e.printStackTrace()
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

    fun fetchRoute(locations: List<Pair<Double, Double>>) {
        viewModelScope.launch {
            if(locations.size < 2) {
                _routePoints.value = emptyList()
                return@launch
            }

            val routePointsToFetch = locations.map { RoutePoint(it.first, it.second) }

            // Repo 호출
            val route = postRepository.getRouteForDay(routePointsToFetch)
            _routePoints.value = route ?: emptyList()
        }
    }

    fun clearRoute() {
        _routePoints.value = emptyList()
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