package com.nemnem.travelapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PointRed,        // Purple80 대신 우리가 만든 PointRed
    secondary = TextSub,       // PurpleGrey80 대신 TextSub
    tertiary = Pink80,
    background = TextMain, // 배경도 테마에 맞춰 설정
    surface = TextMain
)

private val LightColorScheme = lightColorScheme(
    primary = PointRed,
    background = Beige,
    surface = CardWhite,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun TravelAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color 기능을 사용하지 않도록 기본값을 false로 변경
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}