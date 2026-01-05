package com.example.travelapp.util

import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object UtilTime {

    /**
     * ISO 8601 날짜를 "2026.01.05" 형식으로 변환
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatIsoDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val zonedDateTime = ZonedDateTime.parse(isoString)
            // 기기 시스템 언어에 맞는 포맷 설정
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.getDefault())
            zonedDateTime.format(formatter)
        } catch (e: Exception) {
            "날짜 형식 오류"
        }
    }

    /**
     * ISO 8601 날짜를 상대적 시간("3분 전", "2일 전")으로 변환
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatRelativeTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "방금 전"

        return try {
            // 1. 서버 시간(UTC) 파싱
            val zonedDateTime = ZonedDateTime.parse(isoString)
            // 2. 밀리초 단위로 변환
            val timeInMillis = zonedDateTime.toInstant().toEpochMilli()

            // 3. 안드로이드 시스템 내장 함수 사용 (상대 시간 계산)
            DateUtils.getRelativeTimeSpanString(
                timeInMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE // "3분 전"처럼 짧게 표시
            ).toString()
        } catch (e: Exception) {
            // 파싱 실패 시 일반 날짜 형식으로 대체 출력
            formatIsoDateTime(isoString)
        }
    }
}