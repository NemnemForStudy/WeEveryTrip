package com.example.travelapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 폰트 선명도를 위한 공통 설정
private val defaultPlatformStyle = PlatformTextStyle(
    includeFontPadding = false
)

val Typography = Typography(
    // 1. "모여로그", "로그인" 같은 초대형 타이틀
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp, // 자간 조여서 쫀쫀하게
        platformStyle = defaultPlatformStyle
    ),

    // 2. 카드 제목, 섹션 이름
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        platformStyle = defaultPlatformStyle
    ),

    // 3. 닉네임, 중간 강조 텍스트
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        platformStyle = defaultPlatformStyle
    ),

    // 4. 일반 본문 (Medium으로 설정하여 폴드4에서 선명하게 보임)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        platformStyle = defaultPlatformStyle
    ),

    // 5. 날짜, 통계 숫자, 작은 태그 등
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        platformStyle = defaultPlatformStyle
    )
)