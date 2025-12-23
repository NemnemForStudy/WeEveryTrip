package com.example.travelapp.util

// ✅ 모든 패키지를 java.util 및 java.text로 통일합니다.
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    // java.util.TimeZone을 사용하므로 SimpleDateFormat과 호환됩니다.
    private val UTC_ZONE = TimeZone.getTimeZone("UTC")

    /**
     * Long 타입의 시작일과 종료일 사이의 모든 날짜(00:00:00)를 List<Long>으로 반환합니다.
     */
    fun generateDaysBetween(startMillis: Long, endMillis: Long): List<Long> {
        val days = mutableListOf<Long>()
        // java.util.Calendar 사용
        val calendar = Calendar.getInstance(UTC_ZONE).apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = Calendar.getInstance(UTC_ZONE).apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (!calendar.after(endCalendar)) {
            days.add(calendar.timeInMillis)
            calendar.add(Calendar.DATE, 1)
        }
        return days
    }

    fun parseDate(dateString: String): Long? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = UTC_ZONE // 이제 에러 없이 작동합니다.
            }.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun formatToDisplay(millis: Long): String {
        return SimpleDateFormat("MM.dd (E)", Locale.KOREA).apply {
            timeZone = UTC_ZONE
        }.format(Date(millis))
    }
}