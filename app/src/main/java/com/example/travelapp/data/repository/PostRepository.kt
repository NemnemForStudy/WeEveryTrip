package com.example.travelapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import coil.decode.DecodeUtils.calculateInSampleSize
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.GeoJsonPoint
import com.example.travelapp.data.model.Post
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.util.UUID

open class PostRepository @Inject constructor(
    private val postApiService: PostApiService,
    @ApplicationContext private val context: Context
) {
    suspend fun createPost(
        category: String,
        title: String,
        content: String,
        tags: List<String>,
        imageUris: List<Uri>,
        latitude: Double? = null,
        longitude: Double? = null,
        isDomestic: Boolean = true
    ): Result<Post> = withContext(Dispatchers.IO) {
        // 🔥 [핵심 1] withContext(Dispatchers.IO)로 감싸서 백그라운드에서 실행 (앱 안 멈춤)
        return@withContext try {
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsBody = tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())
            val isDomesticBody = isDomestic.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val coordinatesBody = if(latitude != null && longitude != null) {
                val geoPoint = GeoJsonPoint(
                    type = "Point",
                    coordinates = listOf(longitude, latitude)
                )
                val jsonString = Json.encodeToString(geoPoint)
                jsonString.toRequestBody("application/json".toMediaTypeOrNull())
            } else {
                null
            }

            //  이미지 압축 및 변환 로직
            // null 이 반환되면 항목은 리스트에서 제외함. 성공한 이미지만 모아서 리스트로 만듦.
            val imageParts = imageUris.mapNotNull { uri ->
                try {
                    // 비트맵으로 읽어오기 (메모리 절약을 위해 사이즈 확인)
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null

                    // 옵션 설정: 너무 큰 이미지는 줄여서 읽기
                    val options = BitmapFactory.Options()
                    // 메모리 절약을 위한 사이즈 확인 (inJustDecodeBounds)
                    // 비트맵 객체를 생성하지 않고 메타데이터만 읽음.
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    // 적절한 샘플 사이즈 계산 (예: 1024px 정도로 리사이징)
                    val scale = calculateInSampleSize(options, 1024, 1024)

                    // 실제 로딩
                    val options2 = BitmapFactory.Options()
                    // 실제 이미지 메모리에 로딩하는데, 아까 계산한 비율만큼 축소해서 로딩함. 메모리 훨 작게 씀.
                    options2.inSampleSize = scale
                    val realInputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(realInputStream, null, options2)
                    realInputStream?.close()

                    if (bitmap != null) {
                        // 2. 압축해서 임시 파일로 저장 (Quality 70%)
                        val file = File(context.cacheDir, "resized_${UUID.randomUUID()}.jpg")
                        val outputStream = FileOutputStream(file)
                        // 압축 및 임시 파일 저장 - compress, 품질 70%
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        outputStream.flush()
                        outputStream.close()

                        // 3. Multipart 변환
                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("images", file.name, requestFile)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val response = postApiService.createPost(
                category = categoryBody,
                title = titleBody,
                content = contentBody,
                tags = tagsBody,
                images = imageParts,
                coordinates = coordinatesBody,
                isDomestic = isDomesticBody
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(IllegalStateException("API 응답 본문이 비어있습니다."))
            } else {
                Result.failure(RuntimeException("게시물 생성 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if(height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = height / 2

            while(halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // 여기에 open을 해야 mocking이 된다
    open suspend fun searchPostsByTitle(query: String): Result<List<Post>> {
        println("🔍 Repository - 검색 시작: query=$query")
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("검색어를 입력해주세요."))
        }

        return try {
            val response = postApiService.searchPosts(query)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                println("❌ 검색 실패: ${response.code()} - ${response.message()}, 에러: $errorBody")
                return Result.failure(
                    IllegalStateException("검색에 실패했습니다. (${response.code()}: ${response.message()})")
                )
            }

            response.body()?.let { posts ->
                println("✅ 검색 성공: ${posts.size}개의 게시물을 찾았습니다.")
                Result.success(posts)
            } ?: run {
                println("⚠️ 검색 결과가 비어있습니다.")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            println("❌ 검색 중 오류 발생: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    suspend fun getAllPosts(): Result<List<Post>> {
        println("📋 전체 게시물 조회 시작")
        return try {
            val response = postApiService.getAllPosts()
            
            if (!response.isSuccessful) {
                val errorMsg = "게시물 목록을 가져오는데 실패했습니다. (${response.code()}: ${response.message()})"
                println("❌ $errorMsg")
                return Result.failure(IOException(errorMsg))
            }
            
            response.body()?.let { posts ->
                println("✅ ${posts.size}개의 게시물을 불러왔습니다.")
                Result.success(posts)
            } ?: run {
                println("⚠️ 게시물이 없거나 응답 형식이 올바르지 않습니다.")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            val errorMsg = "게시물 목록을 불러오는 중 오류가 발생했습니다: ${e.message}"
            println("❌ $errorMsg")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}