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
        var inputStream: InputStream? = null

        val finalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { MediaStore.setRequireOriginal(uri) } catch (e: Exception) { uri }
        } else { uri }

        inputStream = context.contentResolver.openInputStream(finalUri)
            ?: return null

        return try {
            val exif = ExifInterface(inputStream)
            val latLong = FloatArray(2)

            val hasLocation = exif.getLatLong(latLong)
            inputStream.close()

            if (hasLocation) {
                return Pair(latLong[0].toDouble(), latLong[1].toDouble())
            } else {
                // 🔥 GPS 필수라면 여기서 에러 발생!
                null
            }
        } catch (e: Exception) {
            inputStream.close()
            null
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