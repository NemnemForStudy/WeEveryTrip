package com.nemnem.travelapp.data.model

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ShareUtil {
    /**
     * 비트맵 이미지를 앱 내부 임시 폴더에 저장
     */
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(cachePath, "map_snapshot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    suspend fun shareToInstagramWithMap(
        context: Context,
        mapBitmap: Bitmap,   // 방금 찍은 지도 비트맵
        imageUrls: List<String> // 서버에 있는 원본 사진들
    ) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "인스타그램 연결 중...", Toast.LENGTH_SHORT).show()
        }

        val uris = arrayListOf<Uri>()

        // 1. 지도 스냅샷을 먼저 파일로 저장해서 Uri 추가 (0번 장)
        saveBitmapToCache(context, mapBitmap)?.let { uris.add(it) }

        // 2. 나머지 원본 사진들을 다운로드해서 Uri 추가 (1번 장 ~)
        withContext(Dispatchers.IO) {
            imageUrls.forEachIndexed { index, url ->
                downloadImageToUri(context, url, "share_img_$index")?.let { uris.add(it) }
            }
        }

        if (uris.isEmpty()) return

        // 3. 인스타그램 앱으로 다중 전송 (ACTION_SEND_MULTIPLE)
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent.createChooser(intent, "공유하기"))
        }
    }

    // 카카오톡 전용 버튼 공유
    fun shareToKakao(context: Context, post: Post) {
        val feedTemplate = FeedTemplate(
            content = Content(
                title = post.title,
                description = "${post.nickname}님의 여행기를 확인해보세요!",
                imageUrl = post.imgUrl ?: "", // 게시글 대표 이미지
                link = Link(androidExecutionParams = mapOf("postId" to post.id))
            ),
            buttons = listOf(
                Button(
                    "여행 경로 보기",
                    Link(androidExecutionParams = mapOf("postId" to post.id))
                )
            )
        )

        ShareClient.instance.shareDefault(context, feedTemplate) { sharingResult, error ->
            if (error != null) {
                Log.e("KakaoShare", "공유 실패", error)
            } else if (sharingResult != null) {
                context.startActivity(sharingResult.intent)
            }
        }
    }

    // 인스타 스토리로 공유
    suspend fun shareToInstagram(context: Context, imageUrls: List<String>) {
        if (imageUrls.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "공유할 사진이 없습니다.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "인스타그램으로 연결 중...", Toast.LENGTH_SHORT).show()
        }

        val uris = arrayListOf<Uri>()

        // 1. 모든 사진을 로컬 캐시로 다운로드 (Uri 리스트 생성)
        withContext(Dispatchers.IO) {
            imageUrls.forEachIndexed { index, url ->
                downloadImageToUri(context, url, "share_img_$index")?.let { uris.add(it) }
            }
        }

        if (uris.isEmpty()) return

        // 2. 사진 장수에 따라 Intent 액션 결정
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        // 3. 인스타그램 앱 패키지 지정 및 권한 부여
        intent.apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 인스타그램 앱이 없는 경우 시스템 공유창 활용
            val chooser = Intent.createChooser(intent, "공유하기")
            context.startActivity(chooser)
        }
    }

    // 이미지 URL 다운로드 해 URI 반환하는 헬퍼 함수
    private suspend fun downloadImageToUri(context: Context, url: String, fileName: String): Uri? {
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap ?: return null

            val cachePath = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(cachePath, "$fileName.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    suspend fun shareToInstagramWithMap(
        context: Context,
        mapBitmap: Bitmap,   // 전체 마커가 찍힌 지도 비트맵
        imageUrls: List<String>,
        postId: String
    ) {
        val shareText = "ModuTrip에서 이 여행기를 확인해보세요!\nhttps://modutrip.com/post/$postId"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ModuTrip Post", shareText)
        clipboard.setPrimaryClip(clip)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "본문 내용이 복사되었습니다. 인스타에서 붙여넣기 해주세요!", Toast.LENGTH_LONG).show()
        }

        val uris = arrayListOf<Uri>()

        // 1. 지도 스냅샷 캐시 저장 (0번 장)
        saveBitmapToCache(context, mapBitmap)?.let { uris.add(it) }

        // 2. 원본 사진들 다운로드 및 캐시 저장 (1번 장 ~)
        withContext(Dispatchers.IO) {
            imageUrls.forEachIndexed { index, url ->
                downloadImageToUri(context, url, "share_img_$index")?.let { uris.add(it) }
            }
        }

        if (uris.isEmpty()) return

        // 4. 인스타그램 다중 전송 인텐트 구성
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            val chooser = Intent.createChooser(intent, "인스타그램 공유 선택")
            context.startActivity(intent)
        } catch (e: Exception) {
            // 인스타 미설치 시 시스템 공유창
            val chooser = Intent.createChooser(intent, "ModuTrip 공유하기")
            context.startActivity(chooser)
        }
    }
}