package com.example.travelapp.util

import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object UtilTime {
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatIsoDateTime(isoString: String): String {
        return try {
            val zonedDateTime = ZonedDateTime.parse(isoString)

            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.getDefault())
            zonedDateTime.toLocalDateTime().format(formatter)
        } catch (e: Exception) {
            return "날짜 형식 오류"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatRelativeTime(isoString: String): String {
        return try {
            val zonedDateTime = ZonedDateTime.parse(isoString)
            val instant = zonedDateTime.toInstant()

            val timeInMillis = instant.toEpochMilli()

            DateUtils.getRelativeTimeSpanString(
                timeInMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } catch (e: Exception) {
            formatIsoDateTime(isoString)
        }
    }
}
