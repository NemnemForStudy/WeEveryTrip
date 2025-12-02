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
import java.util.UUID
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

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
        longitude: Double? = null
    ): Result<Post> {
        return try {
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsBody = tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())

            // lat, lon -> coordinate
            val coordinatesBody = if(latitude != null && longitude != null) {
                // 객체 생성
                val geoPoint = GeoJsonPoint(
                    coordinates = listOf(longitude, latitude)
                )
                // GeoJSON 표준 : 경도(lon), 위도(lat) 순서
                // 좌표 정보를 GeoJSON 형식으로 만든다.
                val jsonString = Json.encodeToString(geoPoint) // kotlinx.serialization 사용 가정
                jsonString.toRequestBody("application/json".toMediaTypeOrNull())
            } else {
                null
            }

            // imageUris(List)를 MultipartBody.Part의 Array로 변환합니다.
            val imageParts = imageUris.mapNotNull { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null
                    val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", file.name, requestFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.toTypedArray() // 이 부분이 핵심적인 변경점

            // API 호출 시 위치 정보도 함께 전송
            // 주의: PostApiService.createPost 함수에도 latitude, longitude 인자가 추가되어야 한다.
            val response = postApiService.createPost(
                category = categoryBody,
                title = titleBody,
                content = contentBody,
                tags = tagsBody,
                images = imageParts,
                coordinates = coordinatesBody // API 호출 시 위치 정보 추가
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

    // 여기에 open을 해야 mocking이 된다
    open suspend fun searchPostsByTitle(query: String): Result<List<Post>> {
        println("Repository에서 검색 시작: query=$query")
        return try {
            val response = postApiService.searchPosts(query)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(IllegalStateException("API 응답 본문이 비어있습니다."))
            } else {
                Result.failure(IllegalStateException("검색 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    suspend fun getAllPosts(): Result<List<Post>> {
        return try {
            // PostApiService에 getAllPosts() 함수가 필요합니다.
            // 만약 없다면, 임시로 searchPostsByTitle("") 등을 호출하거나 빈 리스트를 반환
            val response = postApiService.getAllPosts()

            if(response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(IllegalStateException("API 응답 본문이 비어있습니다."))
            } else {
                Result.failure(RuntimeException("전체 조회 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}