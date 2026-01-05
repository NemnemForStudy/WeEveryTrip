package com.example.travelapp.ui.write

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.data.repository.PostRepository
import com.example.travelapp.util.DateUtils
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject


@Serializable
data class ImageLocationMeta(
    // 서버 DB 컬럼명이 day_number/sort_index 이고, 서버는 req.body.imageLocations[i]로 매칭하므로
    // 업로드되는 이미지 순서와 동일한 순서로 만들어서 보내야 함.
    val dayNumber: Int? = null,
    val indexInDay: Int? = null,
    // GPS 없는 사진이면 null (서버에 null로 저장)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null,
    val timeString: String? = null
)
data class PostImage(
    val id: String = UUID.randomUUID().toString(), // 고유 ID
    val uri: Uri,
    val timestamp: Long? = null,
    val timeString: String? = null,
    val dayNumber: Int,
    val latitude: Double? = null,
    val longitude: Double? = null
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

    private val _lastCreatePostId = MutableStateFlow<String?>(null)
    val lastCreatePostId: StateFlow<String?> = _lastCreatePostId.asStateFlow()

    private val _postCreationStatus = MutableStateFlow<PostCreationStatus>(PostCreationStatus.Idle) // 초기 상태 아무것도 하지않음.
    val postCreationStatus: StateFlow<PostCreationStatus> = _postCreationStatus.asStateFlow()

    // 초기화 블록: 날짜가 변경되면 tripDays 자동 계산
    init {
        // combine은 startDate, endDate 둘 중 하나라도 바뀌면 실행됨
        _startDate.combine(_endDate) { start, end ->
            if(start != null && end != null) {
                DateUtils.generateDaysBetween(start, end)
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("PhotoDebug", "2. 처리 시작 - 개수: ${uris.size}")
                val currentStartDate = _startDate.value ?: System.currentTimeMillis()
                val dayInMillis = 24 * 60 * 60 * 1000L

                // 1. 정보 추출 및 리스트 생성
                val newPostImages = uris.map { uri ->
                    val metaData = ExifUtils.extractPhotoInfo(context, uri)
                    val timestamp = metaData?.timestamp
                    val timeString = metaData?.timeString

                    val calculatedDay = if (timestamp != null && timestamp >= currentStartDate) {
                        ((timestamp - currentStartDate) / dayInMillis).toInt() + 1
                    } else {
                        1
                    }

                    PostImage(
                        uri = uri,
                        timestamp = timestamp,
                        timeString = timeString,
                        dayNumber = calculatedDay,
                        latitude = metaData?.position?.latitude,
                        longitude = metaData?.position?.longitude
                    )
                }
                    // 🔥 [추가] 리스트 전체를 시간(timestamp) 순으로 오름차순 정렬
                    .sortedBy { it.timestamp ?: Long.MAX_VALUE }

                val updatedMap = _groupedImages.value.toMutableMap()

                newPostImages.forEach { image ->
                    val day = image.dayNumber
                    val existingList = updatedMap[day] ?: emptyList()

                    if (existingList.none { it.uri == image.uri }) {
                        // 추가된 이미지와 기존 이미지를 합치고 다시 시간순 정렬
                        updatedMap[day] = (existingList + image).sortedBy { it.timestamp ?: Long.MAX_VALUE }
                    }
                }

                withContext(Dispatchers.Main) {
                    _groupedImages.value = updatedMap.toMap()
                    Log.d("PhotoDebug", "3. 상태 업데이트 완료 (시간순 정렬 적용)")
                }
            } catch (e: Exception) {
                Log.e("PhotoDebug", "이미지 처리 에러", e)
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
                // [중요] 업로드 순서는 반드시 "UI에서 보이는 순서(드래그&드랍 반영)"와 동일해야 함.
                // 그래서 imgUris 대신 ViewModel이 관리하는 _groupedImages를 source of truth로 사용.
                val (orderedUris, imageLocationsJson) = if (_groupedImages.value.isNotEmpty()) {
                    buildUploadPayloadFromGroupedImages()
                } else {
                    // 예외 상황: groupedImages가 비어있으면 최소한 인덱스 매칭이 깨지지 않도록 빈 meta로 채움
                    val metaList = imgUris.map { ImageLocationMeta() }
                    Pair(imgUris, Json.encodeToString(metaList))
                }

                val result = postRepository.createPost(
                    category = category,
                    title = title,
                    content = content,
                    tags = tags,
                    // 이미지 업로드 순서(서버의 finalImageUrls 생성 순서)와 metaList 순서가 같아야 함
                    imageUris = orderedUris,
                    // 서버 post.ts가 req.body.imageLocations(JSON string)을 파싱해 post_image에 저장함
                    imageLocationsJson = imageLocationsJson,
                    latitude = currentLat,
                    longitude = currentLon,
                    isDomestic = true,
                    startDateMillis = _startDate.value,
                    endDateMillis = _endDate.value
                )

                result.onSuccess {
                    postRepository.notifyPostChanged()
                    _lastCreatePostId.value = it.id
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

    private fun buildUploadPayloadFromGroupedImages(): Pair<List<Uri>, String> {
        // 1) Day 순서(1,2,3...)로 정렬
        val sortedByDay: Map<Int, List<PostImage>> = _groupedImages.value.toSortedMap()

        // 서버로 업로드할 최종 uri리스트
        val orderedUris = mutableListOf<Uri>()

        // JSON으로 보낼 메타 리스트 (kotlinx serialization 사용)
        val metaList = mutableListOf<ImageLocationMeta>()

        sortedByDay.forEach { (dayNumber, imagesOfDay) ->
            imagesOfDay.forEachIndexed { indexInDay, img ->
                orderedUris += img.uri

                // [중요] 사진별 GPS를 보내야 하므로 img.latitude/img.longitude 를 사용해야 함
                metaList += ImageLocationMeta(
                    dayNumber = dayNumber,
                    indexInDay = indexInDay,
                    latitude = img.latitude,
                    longitude = img.longitude,
                    timestamp = img.timestamp,
                    timeString = img.timeString
                )
            }
        }

        // JSON 문자열로 반환해서 multipart Part 로 보낼 예정
        val imageLocationsJson = Json.encodeToString(metaList)
        return Pair(orderedUris, imageLocationsJson)
    }

    fun removeImage(day: Int, image: PostImage) {
        _groupedImages.value = com.example.travelapp.util.ImageUtil.removeImageFromGrouped(
            currentMap = _groupedImages.value,
            day = day,
            imageToRemove = image
        )
    }
}