package com.example.travelapp.domain.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val id: String,
    val type: String,
    val isDomestic: Boolean
) {
    // ⭐️ TDD Red Phase: 테스트를 컴파일시키기 위한 최소 구현
    // 실제 거리 계산 로직은 다음 단계(Green Phase)에서 Haversine 공식을 사용하여 구현
    // 현재는 0.0을 반환하여 distanceTo 테스트를 실패시킴
    fun distanceTo(other: Location): Double {
        return 0.0
    }

    // ⭐️ TDD Red Phase: 테스트를 컴파일시키기 위한 최소 구현
    // 좌표 유효성 검사 로직은 현재 구현하지 않고 무조건 true를 반환하여 테스트를 실패
    fun isValid(): Boolean {
        return true
    }
}