package com.nemnem.travelapp.domain

import com.nemnem.travelapp.domain.model.Location
import org.junit.Assert.*
import org.junit.Test

/**
 * Location 도메인 모델에 대한 단위 테스트 (JUnit 4 버전)
 *
 * TDD 원칙에 따라 Location 모델의 기본 기능을 검증합니다.
 */
class LocationTest {

    // 더미 데이터들
    private val dummy_id = "TEST_ID_001"
    private val dummy_type = "ATTRACTION"
    private val dummy_is_domestic = true

    @Test
    fun test_location_creation() {
        val latitude = 37.5512
        val longitude = 126.9882
        val name = "남산타워"

        val location = Location(
            latitude = latitude,
            longitude = longitude,
            name = name,
            id = dummy_id,
            type = dummy_type,
            isDomestic = dummy_is_domestic
        )

        // JUnit 4: assertEquals(expected, actual, delta)
        assertEquals(latitude, location.latitude, 0.0001)
        assertEquals(longitude, location.longitude, 0.0001)
        assertEquals(name, location.name)
        assertEquals(dummy_id, location.id)
        assertEquals(dummy_type, location.type)
        assertEquals(dummy_is_domestic, location.isDomestic)
    }

    @Test
    fun test_distanceTo_calculation() {
        val namsan = Location(37.5512, 126.9882, "남산타워", "NAMSAN", dummy_type, dummy_is_domestic)
        val gyeongbok = Location(37.5796, 126.9770, "경복궁", "GYEONBOK", dummy_type, dummy_is_domestic)

        val distance = namsan.distanceTo(gyeongbok)

        // 현재 distanceTo()가 0.0을 반환하므로(Red Phase), 이 테스트는 실패해야 정상입니다.
        assertTrue("거리는 3.0km ~ 4.0km 사이여야 합니다. (현재값: $distance)", distance in 3.0..4.0)
    }

    @Test
    fun test_copy_function() {
        val original = Location(37.5512, 126.9882, "남산타워", "ORIG", dummy_type, dummy_is_domestic)
        val copied = original.copy(name = "N서울타워")

        assertEquals(original.latitude, copied.latitude, 0.0001)
        assertEquals(original.longitude, copied.longitude, 0.0001)
        assertEquals(original.type, copied.type)
        assertEquals(original.isDomestic, copied.isDomestic)
        assertEquals("N서울타워", copied.name)
        assertNotEquals(original, copied)
    }

    @Test
    fun test_location_equality() {
        val location1 = Location(37.5512, 126.9882, "남산타워", "SAME_ID", dummy_type, dummy_is_domestic)
        val location2 = Location(37.5512, 126.9882, "남산타워", "SAME_ID", dummy_type, dummy_is_domestic)

        assertEquals(location1, location2)
        assertEquals(location1.hashCode(), location2.hashCode())
    }

    @Test
    fun test_location_inequality_by_type() {
        val location1 = Location(37.5512, 126.9882, "남산타워", "ID1", "ATTRACTION", true)
        val location2 = Location(37.5512, 126.9882, "남산타워", "ID1", "RESTAURANT", true)

        assertNotEquals(location1, location2)
        assertNotEquals(location1.hashCode(), location2.hashCode())
    }

    @Test
    fun test_isValid_function() {
        val validLocation =
            Location(37.5512, 126.9882, "남산타워", "VALID", dummy_type, dummy_is_domestic)
        val invalidLatitude =
            Location(91.0, 126.9882, "잘못된 위도", "INVALID_LAT", dummy_type, dummy_is_domestic)
        val invalidLongitude =
            Location(37.5512, 181.0, "잘못된 경도", "INVALID_LON", dummy_type, dummy_is_domestic)

        assertTrue(validLocation.isValid())

        // JUnit 4는 메시지가 맨 앞에 옵니다.
        assertFalse("현재 isValid()가 true를 반환하므로 이 테스트는 반드시 실패합니다.", invalidLatitude.isValid())
        assertFalse("현재 isValid()가 true를 반환하므로 이 테스트는 반드시 실패합니다.", invalidLongitude.isValid())
    }
}