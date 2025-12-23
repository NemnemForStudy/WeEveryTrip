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
import com.example.travelapp.data.model.UpdateImageLocationRequest
import com.example.travelapp.data.repository.PostRepository
import com.example.travelapp.ui.common.ImageSelectionHelper
import com.example.travelapp.ui.write.PostImage
import com.example.travelapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val postApiService: PostApiService,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    // 입력 필드 상태
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

    // 태그 상태 추가
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags = _tags.asStateFlow()

    init {
        _startDate.combine(_endDate) { start, end ->
            if(start != null && end != null) {
                DateUtils.generateDaysBetween(start, end)
            } else {
                emptyList()
            }
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

                fetchedPost.imageLocations.forEach { loc ->
                    Log.d("DEBUG_LOAD", "이미지: ${loc.imageUrl}, 시간: ${loc.timestamp}")
                }
                _post.value = fetchedPost
                // 입력 필드 초기화
                _category.value = fetchedPost.category
                _title.value = fetchedPost.title
                _content.value = fetchedPost.content
                _images.value = fetchedPost.images ?: emptyList()
                _isDomestic.value = fetchedPost.isDomestic
                _latitude.value = fetchedPost.latitude
                _longitude.value = fetchedPost.longitude
                _startDate.value = fetchedPost.travelStartDate?.let { DateUtils.parseDate(it) }
                _endDate.value = fetchedPost.travelEndDate?.let { DateUtils.parseDate(it) }
                _tags.value = fetchedPost.tags

                // 에뮬레이터/실기기 분기
                val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86")
                        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))
                val phoneBaseUrl = runCatching {
                    BuildConfig::class.java.getField("PHONE_BASE_URL").get(null) as String
                }.getOrNull()
                val baseUrl = if (isEmulator) {
                    BuildConfig.BASE_URL
                } else {
                    phoneBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL
                }.trimEnd('/') + "/"

                val existingGrouped = fetchedPost.imageLocations
                    .filter { it.dayNumber != null && it.dayNumber > 0 }
                    .mapIndexed { index, loc ->
                        // 서버 URL을 전체 경로로 변환
                        val fullUrl = if (loc.imageUrl.startsWith("http")) {
                            loc.imageUrl
                        } else {
                            "$baseUrl${loc.imageUrl.trimStart('/')}"
                        }
                        PostImage(
                            uri = Uri.parse(fullUrl),
                            timestamp = loc.timestamp ?: -1L,  // 기존 사진은 timestamp 없음 표시
                            dayNumber = loc.dayNumber ?: 1,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                        )
                    }
                    .groupBy { it.dayNumber }
                _groupedImages.value = existingGrouped
            } catch (e: Exception) {
                Log.e("EditPostViewModel", "게시물 로드 실패: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCategory(value: String) {
        _category.value = value
    }

    fun updateTitle(value: String) {
        _title.value = value
    }

    fun updateContent(value: String) {
        _content.value = value
    }

    fun updatePost(postId: String, context: Context) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Loading

            try {
                val allImagesInDrawer = _groupedImages.value.entries
                    .sortedBy { it.key }
                    .flatMap { it.value }

                val localImages = allImagesInDrawer.filter { it.uri.scheme == "content" || it.uri.scheme == "file" }
                val newUrls = if(localImages.isNotEmpty()) {
                    val parts = withContext(Dispatchers.IO) {
                        localImages.map { uriToPart(context, it.uri) }
                    }
                    postApiService.uploadImages(parts).body()?.urls ?: emptyList()
                } else emptyList()

                // 업로드된 URL을 다시 allImagesInDrawer의 로컬 이미지 자리에 매칭
                var newUrlIndex = 0
                // 최종 이미지 목록 = 기존 유지 + 새 업로드
                val finalLocationRequests = allImagesInDrawer.mapIndexed { index, img ->
                    val finalUrl = if(img.uri.scheme == "http" || img.uri.scheme == "https") {
                        img.uri.toString() // 기존 서버 이미지는 그대로 사용
                    } else {
                        newUrls.getOrNull(newUrlIndex++) ?: "" // 새 이미지는 업로드된 URL로 교체
                    }

                    UpdateImageLocationRequest(
                        imageUrl = finalUrl,
                        latitude = img.latitude,
                        longitude = img.longitude,
                        dayNumber = img.dayNumber,
                        sortIndex = index
                    )
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDateStr = _startDate.value?.let { sdf.format(Date(it)) }
                val endDateStr = _endDate.value?.let { sdf.format(Date(it)) }

                val finalImageUrlList = finalLocationRequests.map { it.imageUrl }
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
                    images = finalImageUrlList,
                    imageLocations = finalLocationRequests
                )
                if (result.isSuccess) {
                    _updateStatus.value = UpdateStatus.Success
                } else {
                    _updateStatus.value = UpdateStatus.Error(result.exceptionOrNull()?.message ?: "수정 실패")
                }
            }catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error(e.message ?: "오류 발생")
            }
        }
    }

    fun updateDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun processSelectedImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val existingCoords = _post.value?.imageLocations
                ?.mapNotNull { loc ->
                    if(loc.latitude != null && loc.longitude != null) {
                        Pair(loc.latitude, loc.longitude)
                    } else null
                }?.toSet() ?: emptySet()

            val grouped = ImageSelectionHelper.processUris(
                context = context,
                uris = uris,
                tripDays = _tripDays.value,
                existingCoordinates = existingCoords,
                onLocationDetected = { lat, lon -> updateLocation(lat, lon)}
            )

            // 현재 보관중인 이미지 묶음을 가져와서 수정이 가능한 복사본을 만듦.(원본 데이터 건드리지 않는다.)
            val current = _groupedImages.value.toMutableMap()
            // 새로 분류된 데이터를 하나씩 꺼낸다. day는 날짜(key)이고, images는 그날의 사진 리스트(value)
            grouped.forEach { (day, images) ->
                // 기본 복사본(current)에 해당 날짜 데이터가 이미 있는지 확인함.
                current[day] = (current[day] ?: emptyList()) + images
            }
            // 합치기가 완료된 새 지도를 다시 원본 데이터 변수에 저장하여 화면을 갱신하게 만든다.
            _groupedImages.value = current
        }
    }

    fun swapImages(dayNumber: Int, fromIndex: Int, toIndex: Int) {
        val currentMap = _groupedImages.value.toMutableMap()
        val list = currentMap[dayNumber]?.toMutableList() ?: return

        if(fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            currentMap[dayNumber] = list
            _groupedImages.value = currentMap
        }
    }

    fun updateIsDomestic(value: Boolean) {
        _isDomestic.value = value
    }

    fun updateLocation(lat: Double?, lon: Double?) {
        _latitude.value = lat
        _longitude.value = lon
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    fun updateTags(newTags: List<String>) {
        _tags.value = newTags
    }

    // 최종 이미지 URL 목록 → UpdateImageLocationRequest 리스트 생성
    private fun buildImageLocations(
        finalImages: List<String>,
        newMetaByUrl: Map<String, PostImage>
    ): List<com.example.travelapp.data.model.UpdateImageLocationRequest> {
        // 기존 post에서 가져온 imageLocations 정보를 활용
        val existingLocations = _post.value?.imageLocations ?: emptyList()
        val existingMap = existingLocations.associateBy { it.imageUrl }

        return finalImages.mapIndexed { index, url ->
            val existing = existingMap[url]
            val newMeta = newMetaByUrl[url]

            com.example.travelapp.data.model.UpdateImageLocationRequest(
                imageUrl = url,
                latitude = newMeta?.latitude ?: existing?.latitude,
                longitude = newMeta?.longitude ?: existing?.longitude,
                dayNumber = newMeta?.dayNumber ?: existing?.dayNumber,
                sortIndex = index
            )
        }
    }

    private fun uriToPart(context: Context, uri: Uri): MultipartBody.Part {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val bytes = inputStream.use { it.readBytes() }
        val mime = context.contentResolver.getType(uri) ?: "image/*"
        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("images", "upload.jpg", requestBody)
    }
}
