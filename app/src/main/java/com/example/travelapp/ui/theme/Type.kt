package com.example.travelapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 폰트 선명도를 위한 공통 스타일 설정 (백엔드의 공통 상수로 이해하시면 됩니다)
private val defaultPlatformStyle = PlatformTextStyle(
    includeFontPadding = false // 불필요한 패딩을 제거해 폰트가 뭉개지는 것을 방지
)

val Typography = Typography(
    // 1. "모여로그" 같은 대형 타이틀용
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold, // ✅ Normal -> ExtraBold로 격상
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp, // 자간을 살짝 조여 더 꽉 차 보이게 함
        platformStyle = defaultPlatformStyle
    ),

    // 2. "내 활동", "알림 설정" 같은 섹션 제목용
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // ✅ Normal -> Bold로 격상
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle
    ),

    // 3. 메뉴 이름, 설정 항목 이름용
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // ✅ 중간 두께를 SemiBold로 확실하게
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        platformStyle = defaultPlatformStyle
    ),

    // 4. 일반 본문 글자용
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // ✅ Normal -> Medium으로 한 단계 업그레이드
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        platformStyle = defaultPlatformStyle
    ),

    // 5. 부가 설명, 날짜 등 작은 글자용
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        platformStyle = defaultPlatformStyle
    )
)