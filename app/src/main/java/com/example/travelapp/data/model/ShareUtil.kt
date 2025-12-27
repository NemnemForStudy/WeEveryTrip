package com.example.travelapp.data.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.travelapp.BuildConfig
import com.example.travelapp.util.RetrofitClient
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ShareUtil {
    /**
     * 이미지(지도 캡쳐)와 텍스트를 함께 SNS에 공유.
     * @param context 안드로이드 시스템 기능을 사용하기 위한 맥락 정보
     * @param bitmap 네이버 지도에서 캡처한 비트맵 이미지 데이터
     * @param shareText 링크와 닉네임이 포함된 홍보 문구
     */

    fun sharePost(context: Context, bitmap: Bitmap, shareText: String) {
        // 메모리에 있는 이미지를 실제 파일로 저장하고 그 주소 가져옴
        val imageUrl = saveBitmapToCache(context, bitmap)
        if(imageUrl != null) {
            // Intent 생성. 인텐트는 다른 앱에게 전송할 메시지 박스
            val intent = Intent(Intent.ACTION_SEND).apply {
                // 이미지와 텍스트를 동시에 보낼 때 사용하는 표준 MIME
                type = "image/*"

                // 공유창에 함께 들어갈 텍스트 담음.
                putExtra(Intent.EXTRA_TEXT, shareText)

                // 공유할 이미지 파일의 주소 담음.
                putExtra(Intent.EXTRA_STREAM, imageUrl)

                // 파일을 받는 앱에게 "이 파일을 읽어도 좋다" 는 임시 권한 부여
                // 이 설정 없으면 상대방 앱에서 보안 에러가 나며 이미지 보이지 않음.
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 시스템 공유창 띄움
            context.startActivity(Intent.createChooser(intent, "ModuTrip 여행 공유"))
        }
    }

    /**
     * 비트맵 이미지를 앱 내부 임시 폴더에 저장
     */
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            // 앱 전용 캐시 디렉토리에 'shared_images' 라는 폴더 지정
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs() // 폴더 없으면 생성

            // 폴더 안에 파일명 생성
            val file = File(cachePath, "temp_shared_map.jpg")
            val stream = FileOutputStream(file)

            // 비트맵 데이터 JPEG 형식으로 압축해 파일에 기록
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()

            // FileProvider 통해 파일을 가리키는 안전한 Content Uri 생성
            // 두 번째 인자인 authrities는 메니페스트에 적은 '${applicationId}.fileprovider' 와 정확히 일치해야함.
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // 카카오톡 전용 버튼 공유
    fun shareToKakao(context: Context, post: Post) {
        val finalImageUrl = toFullUrl(post.imgUrl) ?: ""
        android.util.Log.d("KAKAO_SHARE", "전달되는 이미지 URL: $finalImageUrl") // 로그 찍어보기
        if(ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
            val feed = FeedTemplate(
                content = Content(
                    title = post.title,
                    description = "${post.nickname}님의 여행기를 확인해보세요!",
                    imageUrl = finalImageUrl, // 게시글 대표 이미지
                    link = Link(androidExecutionParams = mapOf("postId" to post.id))
                ),
                buttons = listOf(
                    Button(
                        "앱에서 보기",
                        Link(androidExecutionParams = mapOf("postId" to post.id))
                    )
                )
            )

            ShareClient.instance.shareDefault(context, feed) { sharingResult, error ->
                if(error != null) {
                    // 에러 시 일반 공유로 유도
                    Toast.makeText(context, "카카오 공유 실패, 일반 공유를 이용해주세요.", Toast.LENGTH_SHORT).show()
                } else if(sharingResult != null) {
                    context.startActivity(sharingResult.intent)
                }
            }
        } else {
            // 카카오톡 없으면 웹 공유 등 시도하거나 안내
            Toast.makeText(context, "카카오톡이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toFullUrl(urlOrPath: String?): String? {
        if(urlOrPath.isNullOrBlank()) return null
        if(urlOrPath.startsWith("http")) return urlOrPath
        // 현재 사용 중인 ngrok 주소 가져옴
        val baseUrl = resolveBaseUrlForDevice().trimEnd('/')
        val purePath = urlOrPath.trimStart('/')
        val fullUrl = "$baseUrl/$purePath"
        android.util.Log.d("KAKAO_DEBUG", "최종 변환된 전체 주소: $fullUrl")
        return fullUrl
    }

    private fun resolveBaseUrlForDevice(): String {
        val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))

        val phoneBaseUrl = runCatching {
            BuildConfig::class.java.getField("PHONE_BASE_URL").get(null) as String
        }.getOrNull()

        val raw = if(isEmulator) {
            BuildConfig.BASE_URL
        } else {
            phoneBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL
        }

        return raw.trimEnd('/') + "/"
    }

    suspend fun uploadMapCapture(context: Context, bitmap: Bitmap, token: String): String? {
        return try {
            // 비트맵 임시 파일로 저장
            val file = File(context.cacheDir, "share_map_temp.jpg")
            val stream = FileOutputStream(file)
            val isCompressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()

            if (!isCompressed) {
                android.util.Log.e("UPLOAD_DEBUG", "비트맵 압축 실패")
                return null
            }

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("images", file.name,requestFile)
            val imageList = listOf(body)
            val response = RetrofitClient.postApiService.uploadImages("Bearer $token", imageList)

            if (response.isSuccessful) {
                val responseBody = response.body()
                // 서버 응답 DTO에서 urls를 제대로 가져오는지 확인
                val uploadedUrl = responseBody?.urls?.firstOrNull()

                if (uploadedUrl == null) {
                    // HTTP 200은 왔지만, JSON 파싱 결과가 null인 경우 (Key값 불일치 가능성)
                    android.util.Log.e("UPLOAD_DEBUG", "서버 응답 성공했으나 URL이 비어있음. JSON 구조 확인 필요: $responseBody")
                } else {
                    android.util.Log.d("UPLOAD_DEBUG", "최종 업로드 성공 주소: $uploadedUrl")
                }
                uploadedUrl
            } else {
                // [STEP 4] 서버 에러 발생 (4xx, 5xx)
                val errorCode = response.code()
                val errorMessage = response.errorBody()?.string()
                android.util.Log.e("UPLOAD_DEBUG", "서버 에러 발생! 코드: $errorCode, 메시지: $errorMessage")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}