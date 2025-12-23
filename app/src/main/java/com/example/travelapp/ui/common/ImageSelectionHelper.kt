package com.example.travelapp.ui.common

import android.content.Context
import android.net.Uri
import com.example.travelapp.ui.write.PostImage
import com.example.travelapp.util.ExifUtils
import java.util.Calendar

object ImageSelectionHelper {
    fun processUris(
        context: Context,
        uris: List<Uri>,
        tripDays: List<Long>,
        existingCoordinates: Set<Pair<Double, Double>> = emptySet(),
        onLocationDetected: (Double?, Double?) -> Unit = { _, _ -> }
    ): Map<Int, List<PostImage>> {
        if(uris.isEmpty() || tripDays.isEmpty()) return emptyMap()

        val processed = uris.mapNotNull { uri ->
            // try-catch 대신 runCatching 사용해서 짧게 씀.
            val timestamp = runCatching { ExifUtils.extractDate(context, uri) }.getOrNull()
                // uri에 timestamp가 없네? 그럼 이 사진은 무시하고(null 반환), 다음 사진 처리하러 간다 라는 뜻
                ?: return@mapNotNull null
            val dayStart = getDayStartMillis(timestamp)
            val dayIndex = tripDays.indexOf(dayStart)
            val dayNumber = if(dayIndex != -1) dayIndex + 1 else 0
            if(dayNumber <= 0) return@mapNotNull null

            val location = runCatching { ExifUtils.extractLocation(context, uri) }.getOrNull()

            if(location != null && existingCoordinates.contains(location)) {
                return@mapNotNull null // 이미 있는 좌표면 스킵
            }
            PostImage(
                uri = uri,
                timestamp = timestamp,
                dayNumber = dayNumber,
                latitude = location?.first,
                longitude = location?.second,
            )
        }.sortedBy { it.timestamp }

        processed.firstOrNull { it.latitude != null && it.longitude != null }?.let {
            onLocationDetected(it.latitude, it.longitude)
        }

        return processed.groupBy { it.dayNumber }
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
}