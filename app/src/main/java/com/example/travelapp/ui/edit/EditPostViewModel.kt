package com.example.travelapp.ui.edit

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.RoutePoint
import com.example.travelapp.data.model.UpdateImageLocationRequest
import com.example.travelapp.data.repository.PostRepository
import com.example.travelapp.ui.common.ImageSelectionHelper
import com.example.travelapp.ui.write.PostImage
import com.example.travelapp.util.DateUtils
import com.example.travelapp.util.ImageUtil
import com.example.travelapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val postApiService: PostApiService,
    private val postRepository: PostRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    private val _category = MutableStateFlow("")
    val category = _category.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    private val _images = MutableStateFlow<List<String>>(emptyList())
    val images = _images.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude = _longitude.asStateFlow()

    private val _isDomestic = MutableStateFlow<Boolean?>(null)
    val isDomestic = _isDomestic.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate = _endDate.asStateFlow()

    private val _tripDays = MutableStateFlow<List<Long>>(emptyList())
    val tripDays = _tripDays.asStateFlow()

    private val _groupedImages = MutableStateFlow<Map<Int, List<PostImage>>>(emptyMap())
    val groupedImages = _groupedImages.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags = _tags.asStateFlow()

    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    init {
        _startDate.combine(_endDate) { start, end ->
            if(start != null && end != null) DateUtils.generateDaysBetween(start, end)
            else emptyList()
        }.onEach { days ->
            _tripDays.value = days
        }.launchIn(viewModelScope)
    }

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Loading : UpdateStatus()
        object Success : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedPost = postApiService.getPostById(postId)
                _post.value = fetchedPost

                // 입력 필드 초기화
                _category.value = fetchedPost.category ?: ""
                _title.value = fetchedPost.title ?: ""
                _content.value = fetchedPost.content ?: ""
                _images.value = emptyList()
                _isDomestic.value = fetchedPost.isDomestic
                _latitude.value = fetchedPost.latitude
                _longitude.value = fetchedPost.longitude
                _startDate.value = DateUtils.parseDate(fetchedPost.travelStartDate)
                _endDate.value = DateUtils.parseDate(fetchedPost.travelEndDate)
                _tags.value = fetchedPost.tags ?: emptyList()

                // BaseURL 결정 (이미지 경로 복원용)
                val baseUrl = resolveBaseUrl()

                val existingGrouped = fetchedPost.imageLocations
                    .filter { it.dayNumber != null && it.dayNumber > 0 }
                    .map { loc ->
                        val fullUrl = if (loc.imageUrl.startsWith("http")) loc.imageUrl
                        else "$baseUrl${loc.imageUrl.trimStart('/')}"
                        PostImage(
                            uri = Uri.parse(fullUrl),
                            timestamp = loc.timestamp,
                            dayNumber = loc.dayNumber ?: 1,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                        )
                    }
                    .groupBy { it.dayNumber }
                _groupedImages.value = existingGrouped
            } catch (e: Exception) {
                Log.e("EditPostViewModel", "로드 실패: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ 핵심 수정: updatePost 로직 재구성
    fun updatePost(postId: String, context: Context) {
        Log.d("DEBUG_ENTRY", "1. 함수 진입 성공") // 👈 코루틴 밖
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Loading

            try {
                val token = tokenManager.getToken() ?: throw Exception("로그인 정보가 없습니다.")
                val baseUrl = resolveBaseUrl()

                // 1. 모든 이미지를 하나의 리스트로 평탄화
                val allImagesInDrawer = _groupedImages.value.entries
                    .sortedBy { it.key }
                    .flatMap { it.value }
                Log.d("DEBUG", "전송 직전 총 사진 개수: ${allImagesInDrawer.size}")

                // 2. 새로 추가된 로컬 이미지들만 필터링하여 업로드
                val localImages = allImagesInDrawer.filter { it.uri.scheme == "content" || it.uri.scheme == "file" }

                val newUrls = if (localImages.isNotEmpty()) {
                    val parts = withContext(Dispatchers.IO) {
                        localImages.map { uriToPart(context, it.uri) }
                    }
                    val response = postApiService.uploadImages("Bearer $token", parts)
                    if (response.isSuccessful) response.body()?.urls ?: emptyList()
                    else throw Exception("이미지 업로드 실패")
                } else emptyList()

                // 3. 업로드된 URL을 원본 위치에 매칭하여 최종 요청 객체 생성
                var newUrlIndex = 0
                val finalLocationRequests = allImagesInDrawer.mapIndexed { index, img ->
                    val isRemote = img.uri.scheme == "http" || img.uri.scheme == "https"
                    val finalUrl = if (isRemote) {
                        img.uri.toString()
                    } else {
                        newUrls.getOrNull(newUrlIndex++) ?: ""
                    }

                    UpdateImageLocationRequest(
                        imageUrl = finalUrl,
                        latitude = img.latitude,
                        longitude = img.longitude,
                        dayNumber = img.dayNumber,
                        sortIndex = index,
                        timestamp = img.timestamp
                    )
                }.filter { it.imageUrl.isNotEmpty() } // 전송 직전 URL 중복 제거

                // 4. 날짜 포맷팅 및 Repository 호출
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val startDateStr = _startDate.value?.let { sdf.format(Date(it)) }
                val endDateStr = _endDate.value?.let { sdf.format(Date(it)) }

                val result = postRepository.updatePost(
                    postId = postId,
                    category = _category.value,
                    title = _title.value,
                    content = _content.value,
                    tags = _tags.value,
                    latitude = _latitude.value,
                    longitude = _longitude.value,
                    isDomestic = _isDomestic.value,
                    travelStartDate = startDateStr,
                    travelEndDate = endDateStr,
                    images = emptyList(),
                    imageLocations = finalLocationRequests
                )

                if (result.isSuccess) {
                    _updateStatus.value = UpdateStatus.Success
                } else {
                    _updateStatus.value = UpdateStatus.Error(result.exceptionOrNull()?.message ?: "수정 실패")
                }

            } catch (e: Exception) {
                Log.e("EditPostViewModel", "수정 오류: ${e.message}")
                _updateStatus.value = UpdateStatus.Error(e.message ?: "오류 발생")
            }
        }
    }

    private fun resolveBaseUrl(): String {
        val isEmulator = (Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator"))
        val phoneBaseUrl = runCatching {
            BuildConfig::class.java.getField("PHONE_BASE_URL").get(null) as String
        }.getOrNull()

        return (if (isEmulator) BuildConfig.BASE_URL
        else phoneBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL)
            .trimEnd('/') + "/"
    }

    // ... (이하 processSelectedImages, swapImages 등 기존 유틸 함수들은 동일하게 유지)

    fun updateCategory(value: String) { _category.value = value }
    fun updateTitle(value: String) { _title.value = value }
    fun updateContent(value: String) { _content.value = value }
    fun updateDateRange(start: Long?, end: Long?) { _startDate.value = start; _endDate.value = end }
    fun updateIsDomestic(value: Boolean) { _isDomestic.value = value }
    fun updateLocation(lat: Double?, lon: Double?) { _latitude.value = lat; _longitude.value = lon }
    fun resetStatus() { _updateStatus.value = UpdateStatus.Idle }
    fun updateTags(newTags: List<String>) { _tags.value = newTags }

    private fun uriToPart(context: Context, uri: Uri): MultipartBody.Part {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val bytes = inputStream.use { it.readBytes() }
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("images", "upload_${System.currentTimeMillis()}.jpg", requestBody)
    }

    fun processSelectedImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val existingCoords = _post.value?.imageLocations?.mapNotNull { loc ->
                if (loc.latitude != null && loc.longitude != null) Pair(
                    loc.latitude,
                    loc.longitude
                ) else null
            }?.toSet() ?: emptySet()

            val grouped = ImageSelectionHelper.processUris(
                context,
                uris,
                _tripDays.value,
                existingCoords
            ) { lat, lon ->
                updateLocation(lat, lon)
            }

            val current = _groupedImages.value.toMutableMap()
            grouped.forEach { (day, newImages) ->
                val existingImages = current[day] ?: emptyList()
                // 🔥 이미 있는 URI는 제외하고 추가
                val filteredNewImages = newImages.filter { newImg ->
                    existingImages.none { it.uri == newImg.uri }
                }
                current[day] = existingImages + filteredNewImages
            }
            _groupedImages.value = current
        }
    }

    /**
     * 특정 날짜(Day) 내에서 이미지의 순서를 변경하는 함수
     * @param dayNumber 수정할 날짜 번호
     * @param fromIndex 원래 위치
     * @param toIndex 바꿀 위치
     */
    fun swapImages(dayNumber: Int, fromIndex: Int, toIndex: Int) {
        // 1. 현재의 맵 데이터를 복사
        val currentMap = _groupedImages.value.toMutableMap()
        // 2. 해당 날짜의 리스트를 가져와서 수정 가능한 리스트로 변환
        val list = currentMap[dayNumber]?.toMutableList() ?: return

        // 3. 인덱스 범위를 벗어나지 않는지 확인 후 순서 교체
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)

            // 4. 수정한 리스트를 다시 맵에 넣고 StateFlow 업데이트
            currentMap[dayNumber] = list
            _groupedImages.value = currentMap
        }
    }

    fun fetchRoute(locations: List<Pair<Double, Double>>) {
        viewModelScope.launch {
            val points = locations.map { RoutePoint(it.first, it.second) }
            _routePoints.value = points
        }
    }

    fun removeImage(day: Int, image: PostImage) {
        _groupedImages.value = ImageUtil.removeImageFromGrouped(
            currentMap = _groupedImages.value,
            day = day,
            imageToRemove = image
        )
    }
}