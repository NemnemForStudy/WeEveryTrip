package com.example.travelapp.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object ExifUtils {

    // GPS/날짜 정보가 없거나 읽지 못하면 아예 Exception을 던져버림
    fun extractDate(context: Context, uri: Uri): Long {
        var inputStream: InputStream? = null

        // 1. 원본 접근 시도 (권한 없으면 여기서 SecurityException 발생 -> 앱이 잡아서 토스트 띄움)
        val finalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                MediaStore.setRequireOriginal(uri)
            } catch (e: UnsupportedOperationException) {
                uri
            }
        } else {
            uri
        }

        // 2. 스트림 열기 (실패 시 예외 발생으로 중단)
        inputStream = context.contentResolver.openInputStream(finalUri)
            ?: throw IllegalStateException("이미지 파일을 열 수 없습니다.")

        // 3. EXIF 읽기
        val exif = ExifInterface(inputStream)
        val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

        // 스트림 닫기
        inputStream.close()

        if (dateString != null) {
            try {
                // 날짜 포맷 파싱
                val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                return sdf.parse(dateString)?.time ?: throw IllegalStateException("날짜 형식이 잘못되었습니다.")
            } catch (e: Exception) {
                throw IllegalStateException("날짜 정보를 분석할 수 없습니다.")
            }
        }

        // 4. EXIF 없으면 DB 조회 (최후의 수단)
        return queryDateFromMediaStore(context, uri)
            ?: throw IllegalStateException("이 사진에는 날짜 정보가 없어 등록할 수 없습니다.")
    }

    // 위치 정보도 필수라면 여기서 못 가져오면 에러냄
    fun extractLocation(context: Context, uri: Uri): Pair<Double, Double> {
        var inputStream: InputStream? = null

        val finalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { MediaStore.setRequireOriginal(uri) } catch (e: Exception) { uri }
        } else { uri }

        inputStream = context.contentResolver.openInputStream(finalUri)
            ?: throw IllegalStateException("위치 정보를 읽을 수 없습니다.")

        val exif = ExifInterface(inputStream)
        val latLong = FloatArray(2)

        val hasLocation = exif.getLatLong(latLong)
        inputStream.close()

        if (hasLocation) {
            return Pair(latLong[0].toDouble(), latLong[1].toDouble())
        } else {
            // 🔥 GPS 필수라면 여기서 에러 발생!
            throw IllegalStateException("이 사진에는 위치(GPS) 정보가 없습니다.")
        }
    }

    private fun queryDateFromMediaStore(context: Context, uri: Uri): Long? {
        val cursor = context.contentResolver.query(
            uri, arrayOf(MediaStore.Images.Media.DATE_TAKEN), null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val dateTaken = it.getLong(0)
                if (dateTaken > 0) return dateTaken
            }
        }
        return null
    }
}