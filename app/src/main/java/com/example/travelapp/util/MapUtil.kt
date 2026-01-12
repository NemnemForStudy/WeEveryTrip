package com.example.travelapp.util

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.travelapp.BuildConfig
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.OverlayImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 데이터 클래스는 파일 최상단에 유지
data class MarkerItemWithIndex(
    val position: LatLng?,
    val imageUrl: String?,
    val index: Int,
    val isStart: Boolean,
    val isEnd: Boolean
)

data class ClusterGroup(
    val id: String,
    val position: LatLng,
    val items: List<MarkerItemWithIndex>
)

object MapUtil {
    // 1. URL 처리 관련 (여기 resolveBaseUrlForDevice가 있습니다)
    fun toFullUrl(urlOrPath: String?): String? {
        if (urlOrPath.isNullOrEmpty()) return ""

        if (urlOrPath.startsWith("http")) {
            return urlOrPath.replace("/storage/v1/render/", "/storage/v1/object/")
        }

        // /object/public/ 경로가 무료 플랜에서 이미지를 가져오는 기본 경로입니다.
        val supabaseBaseUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/ModuTripPosts/"

        return "${supabaseBaseUrl.trimEnd('/')}/${urlOrPath.trimStart('/')}"
    }

//    private fun resolveBaseUrlForDevice(): String {
//        val isEmulator = Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("google_sdk")
//        val phoneBaseUrl = runCatching {
//            BuildConfig::class.java.getField("PHONE_BASE_URL").get(null) as String
//        }.getOrNull()
//        val raw = if (isEmulator) BuildConfig.BASE_URL else (phoneBaseUrl ?: BuildConfig.BASE_URL)
//        return raw.trimEnd('/') + "/"
//    }

    // 2. [추가] 방향 계산 (Polyline 화살표용)
    fun bearingDeg(a: LatLng, b: LatLng): Float {
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val brng = Math.toDegrees(Math.atan2(y, x))
        return ((brng + 360.0) % 360.0).toFloat()
    }

    // 3. [추가] 경로 단순화 (좌표가 너무 많을 때 성능 최적화용)
    fun simplifyRoute(points: List<LatLng>, maxPoints: Int): List<LatLng> {
        if (points.size <= maxPoints) return points
        if (maxPoints <= 2) return listOf(points.first(), points.last())
        val step = points.size.toDouble() / (maxPoints - 1)
        val simplified = mutableListOf<LatLng>()
        var accumulated = 0.0
        while (accumulated < points.size - 1) {
            simplified += points[accumulated.toInt()]
            accumulated += step
        }
        simplified += points.last()
        return simplified
    }

    // 4. 비트맵 조작 및 포맷팅 (기존과 동일)
    fun createTextMarkerBitmap(text: String, color: Int, sizePx: Int): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val density = sizePx / 100f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.2f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = android.graphics.Color.WHITE
        paint.strokeWidth = 3f * density
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.2f, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 35f * density
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.WHITE
        val fontMetrics = paint.fontMetrics
        val textY = (sizePx / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(text, sizePx / 2f, textY, paint)
        return output
    }

    fun squareCropRounded(src: Bitmap, size: Int, cornerRadiusPx: Float): Bitmap {
        val minDim = Math.min(src.width, src.height)
        val x = (src.width - minDim) / 2
        val y = (src.height - minDim) / 2
        val cropped = Bitmap.createBitmap(src, x, y, minDim, minDim)
        val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerRadiusPx, cornerRadiusPx, paint)
        return output
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTripDateLabel(startDate: String?, dayIndex: Int): String? {
        if (startDate.isNullOrBlank()) return null
        return try {
            // 1. 서버에서 온 "2025-12-30T15:00:00.000Z" (UTC)를 해석
            val instant = java.time.Instant.parse(startDate)

            // 2. 한국 시간대(KST)로 변환 (그러면 12/31 00:00:00 이 됨)
            val zoneId = java.time.ZoneId.of("Asia/Seoul")
            val localDate = instant.atZone(zoneId).toLocalDate()

            // 3. 변환된 한국 날짜에서 dayIndex만큼 더하기
            val targetDate = localDate.plusDays(dayIndex.toLong())

            // 4. "12/31" 형식으로 반환
            targetDate.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"))
        } catch (e: Exception) {
            // 혹시 형식이 다를 경우를 대비한 방어 코드
            try {
                java.time.LocalDate.parse(startDate?.substring(0, 10))
                    .plusDays(dayIndex.toLong())
                    .format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"))
            } catch (e2: Exception) { null }
        }
    }
}

// Composable 헬퍼
@Composable
fun rememberTextMarkerIcon(text: String, color: Color): OverlayImage {
    val density = LocalDensity.current
    val sizePx = with(density) { 45.dp.toPx() }.toInt()
    return remember(text, color) {
        OverlayImage.fromBitmap(MapUtil.createTextMarkerBitmap(text, color.toArgb(), sizePx))
    }
}