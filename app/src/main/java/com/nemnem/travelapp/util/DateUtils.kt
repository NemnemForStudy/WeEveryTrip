package com.nemnem.travelapp.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val UTC_ZONE = TimeZone.getTimeZone("UTC")
    private val KST_ZONE = java.util.TimeZone.getTimeZone("Asia/Seoul")
    /**
     * 서버의 "2026-01-01" 문자열을 받아
     * 정확히 UTC 기준 2026-01-01 00:00:00의 밀리초를 반환합니다.
     */
    fun parseDate(dateString: String?): Long? {
        if (dateString.isNullOrBlank() || dateString == "null") return null

        return try {
            if (dateString.contains("T")) {
                // 1. 서버에서 온 "2026-01-01T15:00:00.000Z" 형식 처리
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.KOREA)
                sdf.timeZone = UTC_ZONE // 서버 데이터가 UTC임을 알림

                val date = sdf.parse(dateString) ?: return null

                // 한국 시간대(KST) 기준으로 '연-월-일'만 추출해서 UTC 0시로 고정
                val calKst = Calendar.getInstance(KST_ZONE).apply {
                    time = date
                }

                Calendar.getInstance(UTC_ZONE).apply {
                    clear()
                    set(calKst.get(java.util.Calendar.YEAR),
                        calKst.get(java.util.Calendar.MONTH),
                        calKst.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

            } else {
                // 2. "2026-01-01" 단순 형식 처리 (기존 로직 유지)
                val parts = dateString.split("-")
                Calendar.getInstance(UTC_ZONE).apply {
                    clear()
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 밀리초를 "yy년 MM월 dd일"로 변환
     */
    fun formatToDisplay(millis: Long?): String {
        if (millis == null) return ""
        val sdf = SimpleDateFormat("yy년 MM월 dd일", java.util.Locale.KOREA)
        sdf.timeZone = UTC_ZONE
        return sdf.format(Date(millis))
    }

    // 시작일~종료일 사이의 날짜 리스트 생성 (기존 유지)
    fun generateDaysBetween(startMillis: Long, endMillis: Long): List<Long> {
        val days = mutableListOf<Long>()
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
}