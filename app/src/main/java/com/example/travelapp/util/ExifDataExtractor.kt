package com.example.travelapp.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

/**
 * EXIF 데이터 추출 유틸리티
 *
 * 사진 파일에 포함된 GPS 정보(위도, 경도)를 추출합니다.
 * 국내 여행(한반도 범위)인지 국외 여행인지 판단합니다.
 */
data class ExifLocationData(
    val latitude: Double?,
    val longitude: Double?,
    val isDomestic: Boolean
)

object ExifDataExtractor {
    // 한반도 범위 상수
    private const val KOREA_MIN_LAT = 33.0  // 남쪽 경계
    private const val KOREA_MAX_LAT = 43.0  // 북쪽 경계
    private const val KOREA_MIN_LON = 124.0 // 서쪽 경계
    private const val KOREA_MAX_LON = 132.0 // 동쪽 경계

    /**
     * URI에서 EXIF 데이터를 추출하여 위도, 경도 반환
     *
     * @param context Android Context
     * @param imageUri 이미지 URI
     * @return ExifLocationData (위도, 경도, 국내/국외 구분)
     */
    fun extractLocationFromUri(context: Context, imageUri: Uri): ExifLocationData {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                val exifInterface = ExifInterface(stream)
                extractLocationFromExif(exifInterface)
            } ?: ExifLocationData(null, null, true)
        } catch (e: Exception) {
            e.printStackTrace()
            ExifLocationData(null, null, true)
        }
    }

    /**
     * ExifInterface에서 GPS 정보 추출
     *
     * @param exifInterface ExifInterface 객체
     * @return ExifLocationData
     */
    private fun extractLocationFromExif(exifInterface: ExifInterface): ExifLocationData {
        // ExifInterface의 getLatLong() 메서드는 Double 배열 반환
        // [0] = 위도, [1] = 경도
        val latLong = exifInterface.latLong

        return if (latLong != null && latLong.size == 2) {
            val latitude = latLong[0]
            val longitude = latLong[1]
            val isDomestic = isKoreanLocation(latitude, longitude)

            ExifLocationData(
                latitude = latitude,
                longitude = longitude,
                isDomestic = isDomestic
            )
        } else {
            ExifLocationData(null, null, true)
        }
    }

    /**
     * 위도, 경도가 한반도 범위 내인지 판단
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 국내(true) / 국외(false)
     */
    private fun isKoreanLocation(latitude: Double, longitude: Double): Boolean {
        return latitude in KOREA_MIN_LAT..KOREA_MAX_LAT &&
                longitude in KOREA_MIN_LON..KOREA_MAX_LON
    }
}