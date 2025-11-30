package com.example.travelapp.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import okio.IOException
import java.io.InputStream

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
            val latLong = exif.latLong

            if(latLong != null) {
                // latLong[0] = 위도, latLong[1] = 경도
                return Pair(latLong[0], latLong[1])
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
}