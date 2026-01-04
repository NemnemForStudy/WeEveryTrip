package com.example.travelapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.naver.maps.geometry.LatLng
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

// 정보를 한 번에 담아 반환할 데이터 클래스
data class PhotoMetaData(
    val position: LatLng?,
    val timestamp: Long?,
    val timeString: String? // "14:30" 형태
)
object ExifUtils {

    // GPS/날짜 정보가 없거나 읽지 못하면 아예 Exception을 던져버림
    fun extractDate(context: Context, uri: Uri): Long {
        // 원본 접근 시도 (권한 없으면 여기서 SecurityException 발생 -> 앱이 잡아서 토스트 띄움)
        val finalUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { MediaStore.setRequireOriginal(uri) } catch (e: Exception) { uri }
        } else uri

        // 2. 스트림 열기 (실패 시 예외 발생으로 중단)
        context.contentResolver.openInputStream(finalUri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

            if(dateString != null) {
                return try {
                    // EXIF 표준 포맷은 Local.US 사용하는 것이 안전
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    sdf.parse(dateString)?.time ?: throw java.lang.IllegalStateException()
                } catch (e: Exception) {
                    throw IllegalStateException("날짜 형식이 올바르지 않습니다.")
                }
            }
        } ?: throw IllegalStateException("이미지 파일을 열 수 없습니다.")

        return queryDateFromMediaStore(context, uri)
            ?: throw IllegalStateException("날짜 정보가 없는 사진입니다.")
    }

    // 위치 정보도 필수라면 여기서 못 가져오면 에러냄
    fun extractLocation(context: Context, uri: Uri): Pair<Double, Double>? {
        val hasPermission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

        val finalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasPermission) {
            try { MediaStore.setRequireOriginal(uri) } catch (e: Exception) { uri }
        } else { uri }

        return try {
            context.contentResolver.openInputStream(finalUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = FloatArray(2)

                if (exif.getLatLong(latLong)) {
                    Pair(latLong[0].toDouble(), latLong[1].toDouble())
                } else {
                    // 🔥 GPS 필수라면 여기서 에러 발생!
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun extractTimestamp(context: Context, uri: Uri): Long? {
        return try {
            extractDate(context, uri)
        } catch (e: Exception) {
            null
        }
    }

    fun extractPhotoInfo(context: Context, uri: Uri): PhotoMetaData? {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

        val finalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasPermission) {
            try { MediaStore.setRequireOriginal(uri) } catch (e: Exception) { uri }
        } else uri

        return try {
            context.contentResolver.openInputStream(finalUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)

                // 1. 위치 추출
                val latLong = FloatArray(2)
                val position = if (exif.getLatLong(latLong)) LatLng(latLong[0].toDouble(), latLong[1].toDouble()) else null

                // 2. 날짜 및 시간 문자열 추출 (표준 태그들 순차 확인)
                val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)

                var timestamp: Long? = null
                var timeString: String? = null

                if (dateString != null) {
                    try {
                        // EXIF 날짜 포맷 (2025:12:31 14:30:05)
                        val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                        val date = sdf.parse(dateString)
                        timestamp = date?.time

                        // UI에 표시할 "14:30" 추출
                        timeString = date?.let { SimpleDateFormat("HH:mm", Locale.KOREA).format(it) }
                    } catch (e: Exception) {
                        Log.e("ExifUtils", "날짜 파싱 실패: $dateString")
                    }
                }

                // EXIF에 없으면 MediaStore에서 최종 시도 (날짜만)
                if (timestamp == null) {
                    timestamp = queryDateFromMediaStore(context, uri)
                }

                PhotoMetaData(position, timestamp, timeString)
            }
        } catch (e: Exception) {
            Log.e("ExifUtils", "이미지 처리 에러: ${e.message}")
            null
        }
    }


    private fun queryDateFromMediaStore(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATE_TAKEN),
                null,
                null,
                null
            )?.use { cursor -> // 이름을 cursor로 지정했으므로
                if (cursor.moveToFirst()) { // it이 아니라 cursor를 사용해야 함
                    val index = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    if (index != -1) {
                        cursor.getLong(index).takeIf { it > 0 }
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}