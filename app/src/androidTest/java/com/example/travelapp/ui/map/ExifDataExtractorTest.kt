package com.example.travelapp.ui.map

import com.example.travelapp.util.ExifLocationData
import org.junit.Assert.*
import org.junit.Test

/**
 * EXIF 데이터 추출 유틸리티 테스트
 *
 * TDD 방식으로 작성된 단위 테스트입니다.
 */
class ExifDataExtractorTest {
    /**
     * 테스트 1: 한반도 범위 내 좌표 판단 (국내 여행)
     *
     * 서울 좌표: 위도 37.5665, 경도 126.9780
     * 예상 결과: isDomestic = true
     */
    @Test
    fun testKoreanLocationDetection_Seoul() {
        val latitude = 37.5665
        val longitude = 126.9780
        val result = ExifLocationData(
            latitude = latitude,
            longitude = longitude,
            isDomestic = true
        )

        assertTrue("서울은 국내 여행이어야 함.", result.isDomestic)
        assertEquals("위도 확인", 37.5665, result.latitude!!, 0.0001)
        assertEquals("경도 확인", 126.9780, result.longitude!!, 0.0001)
    }

    /**
     * 테스트 2: 한반도 범위 외 좌표 판단 (국외 여행)
     *
     * 도쿄 좌표: 위도 35.6762, 경도 139.6503
     * 예상 결과: isDomestic = false
     */
    @Test
    fun testInternationalLocationDetection_Tokyo() {
        val latitude = 35.6762
        val longitude = 139.6503

        val result = ExifLocationData(
            latitude = latitude,
            longitude = longitude,
            isDomestic = false
        )

        assertFalse("도쿄는 국외 여행이어야 함", result.isDomestic)
        assertEquals("위도 확인", 35.6762, result.latitude!!, 0.0001)
        assertEquals("경도 확인", 139.6503, result.longitude!!, 0.0001)
    }

    /**
     * 테스트 3: EXIF 데이터 없는 이미지 처리
     *
     * GPS 정보가 없는 이미지는 latitude, longitude가 null
     * 예상 결과: isDomestic = true (기본값)
     */
    @Test
    fun testNoExifData() {
        val result = ExifLocationData(
            latitude = null,
            longitude = null,
            isDomestic = true
        )

        assertNull("GPS 정보가 없으면 latitude는 null", result.latitude)
        assertNull("GPS 정보가 없으면 longitude는 null", result.longitude)
        assertTrue("기본값은 국내 여행", result.isDomestic)
    }

    /**
     * 테스트 4: 경계값 테스트 (제주도 최남단)
     *
     * 제주도 최남단: 위도 33.1, 경도 126.5
     * 예상 결과: isDomestic = true (한반도 범위 내)
     */
    @Test
    fun testBoundaryCase_JejuSouthernmost() {
        val latitude = 33.1
        val longitude = 126.5

        val result = ExifLocationData(
            latitude = latitude,
            longitude = longitude,
            isDomestic = true
        )
        assertTrue("제주도는 국내 여행", result.isDomestic)
        assertEquals("위도 확인", 33.1, result.latitude!!, 0.0001)
        assertEquals("경도 확인", 126.5, result.longitude!!, 0.0001)
    }

    /**
     * 테스트 5: 경계값 테스트 (한반도 최북단)
     *
     * 한반도 최북단: 위도 42.5, 경도 130.0
     * 예상 결과: isDomestic = true (한반도 범위 내)
     */
    @Test
    fun testBoundaryCase_KoreaNorthernmost() {
        val latitude = 42.5
        val longitude = 130.0

        val result = ExifLocationData(
            latitude = latitude,
            longitude = longitude,
            isDomestic = true
        )

        assertTrue("한반도 최북단은 국내 여행", result.isDomestic)
    }

    /**
     * 테스트 6: 경계값 테스트 (범위 밖 - 중국)
     *
     * 중국 베이징: 위도 39.9042, 경도 116.4074
     * 예상 결과: isDomestic = false (한반도 범위 외)
     */
    @Test
    fun testOutOfBoundary_Beijing() {
        val latitude = 39.9042
        val longitude = 116.4074

        val result = ExifLocationData(
            latitude = latitude,
            longitude = longitude,
            isDomestic = false
        )

        assertFalse("베이징은 국외 여행", result.isDomestic)
    }
}