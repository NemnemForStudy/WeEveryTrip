package com.example.travelapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

//@HiltViewModel
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

            // 위치 정보도 RequestBody로 변환(null이면 안 보냄)
            val latBody = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lonBody = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

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
            }.toTypedArray() // 이 부분이 핵심적인 변경점입니다.

            // ⭐️ [수정] API 호출 시 위치 정보도 함께 전송
            // 주의: PostApiService.createPost 함수에도 latitude, longitude 인자가 추가되어야 합니다.
            // 만약 API가 아직 위치를 안 받는다면 latBody, lonBody는 빼고 보내세요.
            val response = postApiService.createPost(
                category = categoryBody,
                title = titleBody,
                content = contentBody,
                tags = tagsBody,
                images = imageParts
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

    suspend fun searchPostsByTitle(query: String): Result<List<Post>> {
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
                Result.failure(IllegalStateException("전체 조회 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}