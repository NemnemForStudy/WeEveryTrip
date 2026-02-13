package com.nemnem.travelapp.data.model

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
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(cachePath, "map_snapshot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    // 카카오톡 공유
    fun shareToKakao(context: Context, post: Post) {
        val displayImageUrl = if (!post.imgUrl.isNullOrEmpty()) post.imgUrl 
                             else post.images?.firstOrNull() ?: ""

        val feedTemplate = FeedTemplate(
            content = Content(
                title = post.title,
                description = "${post.nickname}님의 여행기를 확인해보세요!",
                imageUrl = displayImageUrl,
                link = Link(androidExecutionParams = mapOf("postId" to post.id))
            ),
            buttons = listOf(
                Button("여행 경로 보기", Link(androidExecutionParams = mapOf("postId" to post.id)))
            )
        )

        ShareClient.instance.shareDefault(context, feedTemplate) { sharingResult, error ->
            if (error != null) {
                Log.e("KakaoShare", "공유 실패", error)
                Toast.makeText(context, "카카오톡 공유 실패", Toast.LENGTH_SHORT).show()
            } else if (sharingResult != null) {
                context.startActivity(sharingResult.intent)
            }
        }
    }

    // 인스타그램 피드 공유
    suspend fun shareToInstagramFeed(context: Context, mapBitmap: Bitmap, imageUrls: List<String>) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "인스타그램 게시물로 연결 중...", Toast.LENGTH_SHORT).show()
        }

        val uris = arrayListOf<Uri>()
        saveBitmapToCache(context, mapBitmap)?.let { uris.add(it) }

        withContext(Dispatchers.IO) {
            imageUrls.forEachIndexed { index, url ->
                downloadImageToUri(context, url, "share_img_$index")?.let { uris.add(it) }
            }
        }

        if (uris.isEmpty()) return

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

    // 인스타그램 스토리 공유
    suspend fun shareToInstagramStory(context: Context, backgroundBitmap: Bitmap) {
        val backgroundUri = saveBitmapToCache(context, backgroundBitmap) ?: return

        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(backgroundUri, "image/*")
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "인스타그램 앱이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

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
}
