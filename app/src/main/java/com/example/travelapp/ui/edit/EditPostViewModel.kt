package com.example.travelapp.ui.edit

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.repository.PostRepository
import com.example.travelapp.ui.write.PostImage
import com.example.travelapp.util.ExifUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
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

    init {
        _startDate.combine(_endDate) { start, end ->
            if(start != null && end != null) {
                generateDaysBetween(start, end)
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
                _post.value = fetchedPost
                // 입력 필드 초기화
                _category.value = fetchedPost.category
                _title.value = fetchedPost.title
                _content.value = fetchedPost.content
                _images.value = fetchedPost.images ?: emptyList()
                _isDomestic.value = fetchedPost.isDomestic
                _latitude.value = fetchedPost.latitude
                _longitude.value = fetchedPost.longitude
                _startDate.value = fetchedPost.travelStartDate?.let { parseDate(it) }
                _endDate.value = fetchedPost.travelEndDate?.let { parseDate(it) }
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

    fun updatePost(postId: String) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Loading

            val result = postRepository.updatePost(
                postId = postId,
                category = _category.value,
                title = _title.value,
                content = _content.value,
                latitude = _latitude.value,
                longitude = _longitude.value,
                isDomestic = _isDomestic.value
            )

            if (result.isSuccess) {
                _updateStatus.value = UpdateStatus.Success
            } else {
                _updateStatus.value = UpdateStatus.Error(
                    result.exceptionOrNull()?.message ?: "수정 실패"
                )
            }
        }
    }

    fun updateDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun processSelectedImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val currentTripDays = _tripDays.value

            val processedImages = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    val time = ExifUtils.extractDate(context, uri) ?: 0L
                    val dayStartMillis = getDayStartMillis(time)
                    val index = currentTripDays.indexOf(dayStartMillis)
                    val dayNumber = if (index != -1) index + 1 else 0

                    PostImage(
                        uri = uri,
                        timestamp = time,
                        dayNumber = dayNumber
                    )
                }
                    .filter { it.dayNumber > 0 }
                    .sortedBy { it.timestamp }
            }

            val newGrouped = processedImages.groupBy { it.dayNumber }
            _groupedImages.value = newGrouped

            if(processedImages.isNotEmpty()) {
                val firstUri = processedImages.first().uri
                val location = ExifUtils.extractLocation(context, firstUri)
                if(location != null) {
                    updateLocation(location.first, location.second)
                }
            }
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

    private fun generateDaysBetween(startMillis: Long, endMillis: Long): List<Long> {
        val days = mutableListOf<Long>()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startMillis
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

        while(!calendar.after(endCalendar)) {
            days.add(calendar.timeInMillis)
            calendar.add(Calendar.DATE, 1)
        }
        return days
    }

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

    private fun parseDate(dateString: String): Long? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .parse(dateString)?.time
        } catch (e: Exception) {
            null
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
}
