package com.example.travelapp.util

import android.graphics.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.naver.maps.map.overlay.OverlayImage

/**
 * [수정] 방문 순서(index) 파라미터를 추가했습니다.
 */
@Composable
fun rememberClusteredPhotoIcon(
    imageUrl: String?,
    index: Int,         // 방문 순서 추가
    count: Int,         // 해당 지점 사진 수
    sizePx: Int,
    isSelected: Boolean
): OverlayImage? {
    val context = LocalContext.current

    // imageUrl이나 index가 바뀔 때마다 다시 실행
    return produceState<OverlayImage?>(initialValue = null, key1 = imageUrl, key2 = index, key3 = isSelected) {
        if (imageUrl.isNullOrBlank()) {
            value = null
            return@produceState
        }

        val fullUrl = MapUtil.toFullUrl(imageUrl) ?: return@produceState

        val request = ImageRequest.Builder(context)
            .data(fullUrl)
            .size(sizePx)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        if (result is SuccessResult) {
            val bmp = result.drawable.toBitmap()
            // 고도화된 비트맵 생성 로직 호출
            value = OverlayImage.fromBitmap(makeImprovedNumberedMarker(bmp, index, count, isSelected))
        } else {
            value = null
        }
    }.value
}

/**
 * [수정] UI를 훨씬 세련되게 다듬은 마커 생성 함수입니다.
 * Color.White.toArgb() 에러를 해결하기 위해 android.graphics.Color를 사용하거나
 * 정확한 Compose Color.White.toArgb()를 사용합니다.
 */
private fun makeImprovedNumberedMarker(
    photo: Bitmap,
    index: Int,
    count: Int,
    isSelected: Boolean
): Bitmap {
    val size = 180 // 전체 캔버스 크기
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. 선택 시 그림자 효과
    if (isSelected) {
        paint.setShadowLayer(12f, 0f, 6f, 0x66000000)
    }

    // 2. 흰색 프레임 (배경 테두리)
    val frameRect = RectF(20f, 20f, 160f, 160f)
    paint.color = android.graphics.Color.WHITE // Native Color 사용으로 에러 방지
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(frameRect, 30f, 30f, paint)
    paint.clearShadowLayer()

    // 3. 메인 사진 (중앙 배치)
    val photoSize = 120
    val photoBmp = MapUtil.squareCropRounded(photo, photoSize, 20f)
    canvas.drawBitmap(photoBmp, 30f, 30f, null)

    // 4. 방문 순서 번호 배지 (좌측 상단)
    // 선택되었을 때는 강조 색상(Blue), 아닐 때는 어두운 색상
    paint.color = if (isSelected) android.graphics.Color.parseColor("#4A90E2") else android.graphics.Color.parseColor("#222222")
    canvas.drawCircle(45f, 45f, 32f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 34f
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.DEFAULT_BOLD

    // 텍스트 중앙 정렬 계산
    val fontMetrics = paint.fontMetrics
    val textY = 45f - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(index.toString(), 45f, textY, paint)

    // 5. 클러스터 숫자 배지 (사진이 2장 이상일 때만 우측 하단)
    if (count > 1) {
        paint.color = android.graphics.Color.parseColor("#FF4B4B") // 빨간색 배지
        val badgeRect = RectF(110f, 110f, 175f, 175f)
        canvas.drawRoundRect(badgeRect, 15f, 15f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 24f
        val badgeTextY = 142.5f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText("+$count", 142.5f, badgeTextY, paint)
    }

    return output
}