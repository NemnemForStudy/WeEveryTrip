package com.example.travelapp.domain

import com.example.travelapp.domain.model.Location
// ⭐️ 변경점 1: JUnit 4의 Assert 대신 JUnit 5의 Assertions를 사용합니다.
import org.junit.jupiter.api.Assertions.*
// ⭐️ 변경점 2: Test 어노테이션도 Jupiter(JUnit 5) 패키지를 사용합니다.
import org.junit.jupiter.api.Test

/**
 * Location 도메인 모델에 대한 단위 테스트 (JUnit 5 버전)
 *
 * TDD 원칙에 따라 Location 모델의 기본 기능을 검증합니다.
 */
class LocationTest {

    // 더미 데이터들
    private val dummy_id = "TEST_ID_001"
    private val dummy_type = "ATTRACTION"
    private val dummy_is_domestic = true

    @Test
    fun `Location 객체가 올바르게 생성되는지 테스트`() {
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

        // JUnit 5에서는 delta(허용 오차)가 세 번째 인자로 들어갑니다. (순서 동일)
        assertEquals(latitude, location.latitude, 0.0001)
        assertEquals(longitude, location.longitude, 0.0001)
        assertEquals(name, location.name)
        assertEquals(dummy_id, location.id)
        assertEquals(dummy_type, location.type)
        assertEquals(dummy_is_domestic, location.isDomestic)
    }

    @Test
    fun `두 Location 간의 거리 계산하는 distanceTo 함수 테스트`() {
        val namsan = Location(37.5512, 126.9882, "남산타워", "NAMSAN", dummy_type, dummy_is_domestic)
        val gyeongbok = Location(37.5796, 126.9770, "경복궁", "GYEONBOK", dummy_type, dummy_is_domestic)

        val distance = namsan.distanceTo(gyeongbok)

        // 현재 distanceTo()가 0.0을 반환하므로(Red Phase), 이 테스트는 실패해야 정상입니다.
        assertTrue(distance in 3.0..4.0, "거리는 3.0km ~ 4.0km 사이여야 합니다. (현재값: $distance)")
    }

    @Test
    fun `Location의 copy 함수가 올바르게 동작하는지 테스트`() {
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
    fun `같은 값을 가진 Location 객체는 동일해야함`() {
        val location1 = Location(37.5512, 126.9882, "남산타워", "SAME_ID", dummy_type, dummy_is_domestic)
        val location2 = Location(37.5512, 126.9882, "남산타워", "SAME_ID", dummy_type, dummy_is_domestic)

        assertEquals(location1, location2)
        assertEquals(location1.hashCode(), location2.hashCode())
    }

    @Test
    fun `새로운 필드인 Type이 다르면 Location 객체는 동일하지 않아야 함`() {
        val location1 = Location(37.5512, 126.9882, "남산타워", "ID1", "ATTRACTION", true)
        val location2 = Location(37.5512, 126.9882, "남산타워", "ID1", "RESTAURANT", true)

        assertNotEquals(location1, location2)
        assertNotEquals(location1.hashCode(), location2.hashCode())
    }

    @Test
    fun `유효한 좌표 범위를 검증하는 isValid 함수 테스트`() {
        val validLocation =
            Location(37.5512, 126.9882, "남산타워", "VALID", dummy_type, dummy_is_domestic)
        val invalidLatitude =
            Location(91.0, 126.9882, "잘못된 위도", "INVALID_LAT", dummy_type, dummy_is_domestic)
        val invalidLongitude =
            Location(37.5512, 181.0, "잘못된 경도", "INVALID_LON", dummy_type, dummy_is_domestic)

        assertTrue(validLocation.isValid())

        // JUnit 5에서는 메시지가 마지막 인자로 옵니다. (JUnit 4는 첫 번째)
        // 하지만 코틀린에서는 람다나 네임드 아규먼트를 쓰지 않으면 헷갈릴 수 있으니
        // Jupiter의 Assertions.assertFalse(condition, message) 순서에 주의하세요.
        // 여기서는 간단하게 조건만 검사하거나 메시지를 뒤로 보냅니다.
        assertFalse(invalidLatitude.isValid(), "현재 isValid()가 true를 반환하므로 이 테스트는 반드시 실패합니다.")
        assertFalse(invalidLongitude.isValid(), "현재 isValid()가 true를 반환하므로 이 테스트는 반드시 실패합니다.")
    }
}