package com.example.travelapp.util

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import okio.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 이미지 파일의 메타데이터(EXIF)를 다루는 유틸리티 객체입니다.
 * 사진에 포함된 GPS 위치 정보를 추출하는 데 사용됩니다.
 */
object ExifUtils {
    /**
     * 주어진 이미지 URI에서 위도(Latitude)와 경도(Longitude)를 추출합니다.
     *
     * @param context ContentResolver를 얻기 위한 컨텍스트
     * @param uri 이미지 파일의 URI
     * @return 위치 정보가 있으면 Pair(latitude, longitude), 없으면 null 반환
     */
    fun extractLocation(context: Context, uri: Uri): Pair<Double, Double>? {
        var inputStream: InputStream? = null
        try {
            // URI로부터 스트림을 염.
            inputStream = context.contentResolver.openInputStream(uri)
            if(inputStream == null) return null

            // 메타 데이터 읽어옴
            val exif = ExifInterface(inputStream)

            // getLatLong()은 위도, 경도 배열(double[2]) 반환하거나 정보 없으면 null 반환
            val latLong = FloatArray(2)
            val hasLatLong = exif.getLatLong(latLong)

            if(hasLatLong) {
                // latLong[0] = 위도, latLong[1] = 경도
                return Pair(latLong[0].toDouble(), latLong[1].toDouble())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * 이미지에서 촬영 일시(DateTimeOriginal)를 추출하여 밀리초(Long)로 반환합니다.
     * 날짜 정보가 없으면 null을 반환합니다.
     */
    fun extractDate(context: Context, uri: Uri): Long? {
        var inputStream: InputStream? = null
        try {
            val finalUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    MediaStore.setRequireOriginal(uri)
                } catch (e: UnsupportedOperationException) {
                    uri
                }
            } else {
                uri
            }

            inputStream = context.contentResolver.openInputStream(finalUri)
            if(inputStream != null) {
                val exif = ExifInterface(inputStream)
                // 촬영 시간 태그 읽기
                val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?:exif.getAttribute(ExifInterface.TAG_DATETIME)

                if(dateString != null) {
                    // 날짜 형식을 파싱해 Long 변환
                    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
                    return sdf.parse(dateString)?.time
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return null
    }
}