package com.example.travelapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.GeoJsonPoint
import com.example.travelapp.data.model.Post
import dagger.hilt.android.qualifiers.ApplicationContext
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
    ): Result<Post> {
        println("📝 게시물 생성 시작 - 제목: $title, 이미지 개수: ${imageUris.size}")
        
        // 필수 필드 검증
        if (title.isBlank() || content.isBlank()) {
            val error = "제목과 내용은 필수 입력 사항입니다."
            println("❌ $error")
            return Result.failure(IllegalArgumentException(error))
        }

        return try {
            // 요청 본문 생성
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsBody = tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())
            val isDomesticBody = isDomestic.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // 위치 정보 처리
            val coordinatesBody = if (latitude != null && longitude != null) {
                println("📍 위치 정보 포함: 위도=$latitude, 경도=$longitude")
                val geoPoint = GeoJsonPoint(
                    type = "Point",
                    coordinates = listOf(longitude, latitude))
                val jsonString = Json.encodeToString(geoPoint)
                jsonString.toRequestBody("application/json".toMediaTypeOrNull())
            } else {
                println("ℹ️ 위치 정보 없음")
                null
            }

            // 이미지 처리
            println("🖼️ 이미지 처리 중... (${imageUris.size}개)")
            val imageParts = imageUris.mapIndexed { index, uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val file = File(context.cacheDir, "img_${System.currentTimeMillis()}_$index.jpg")
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("images", file.name, requestFile)
                    } ?: throw IOException("이미지 파일을 열 수 없습니다: $uri")
                } catch (e: Exception) {
                    println("⚠️ 이미지 처리 실패 (${uri.lastPathSegment}): ${e.message}")
                    throw IOException("이미지 처리 중 오류가 발생했습니다: ${e.message}", e)
                }
            }
            
            if (imageParts.isEmpty()) {
                println("⚠️ 유효한 이미지가 없습니다. 빈 리스트로 계속 진행합니다.")
            }

            println("🚀 서버에 게시물 전송 중...")
            val response = postApiService.createPost(
                category = categoryBody,
                title = titleBody,
                content = contentBody,
                tags = tagsBody,
                images = imageParts,
                coordinates = coordinatesBody,
                isDomestic = isDomesticBody
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMsg = "게시물 생성 실패 (${response.code()}): ${response.message()}\n$errorBody"
                println("❌ $errorMsg")
                return Result.failure(IOException(errorMsg))
            }

            response.body()?.let { post ->
                println("✅ 게시물이 성공적으로 생성되었습니다. ID: ${post.id}")
                Result.success(post)
            } ?: run {
                val errorMsg = "서버 응답이 올바르지 않습니다."
                println("❌ $errorMsg")
                Result.failure(IllegalStateException(errorMsg))
            }
            
        } catch (e: Exception) {
            val errorMsg = "게시물 생성 중 오류 발생: ${e.message}"
            println("❌ $errorMsg")
            e.printStackTrace()
            Result.failure(e)
        }
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