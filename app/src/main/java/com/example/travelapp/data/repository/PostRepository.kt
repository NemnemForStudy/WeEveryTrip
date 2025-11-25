package com.example.travelapp.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.CreatePostRequest
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

// Hilt가 이 Repo를 생성할 때 필요한 의존성을 자동으로 주입할 수 있도록 @Inject 사용
// PostApiService는 NetworkModule에서 @Provides로 제공되므로 Hilt가 이를 주입해 줄 수 있다.
class PostRepository @Inject constructor(
    private val postApiService: PostApiService, // Retrofit으로 생성된 PostApiServices 인스턴스 주입
    @ApplicationContext private val context: Context
) {
    // 게시물 생성하는 함수
    // suspend 키워드 사용해 코루틴 내 비동기적으로 실행될 수 있도록 함.
    // Result<Post> 반환해 성공 or 실패 상태와 함께 데이터 안전하게 전달함.
    suspend fun createPost(
        category: String,
        title: String,
        content: String,
        tags: List<String>,
        imageUris: List<Uri> // 이미지 uri 리슽트 받음.
    ): Result<Post> {
        return try {
            // 텍스트 데이터를 RequestBody로 변환해 Map에 담음.
            val textData = mutableMapOf<String, okhttp3.RequestBody>()
            textData["category"] = category.toRequestBody("text/plain".toMediaTypeOrNull())
            textData["title"] = title.toRequestBody("text/plain".toMediaTypeOrNull())
            textData["content"] = content.toRequestBody("text/plain".toMediaTypeOrNull())
            // 태그 리스트를 JSON 문자열로 변환해 추가할 수 있음
            // 여기서는 간단하게 쉼표로 구분된 문자열로 보냄.
            textData["tags"] = tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())

            // 이미지 Uri 리스트를 MultipartBody.Part 리스트로 변환
            val imageParts = imageUris.mapNotNull { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null

                    // 고유 파일 이름 가진 임시 파일 생성
                    val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
                    val outputStream = FileOutputStream(file)

                    // 3. InputStream에서 읽은 데이터를 FileOutputStream을 통해 파일에 쓴다.
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    // 생성된 파일 RequestBody로 만들고 변환
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    // 서버에서 images 라는 이름으로 받을 준비가 되어 있어야 함.
                    MultipartBody.Part.createFormData("images", file.name, requestFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            // 수정된 API 호출
            val response = postApiService.createPost(textData, imageParts)

            if(response.isSuccessful) {
                // 응답이 성공적이면 본문(body)를 Result.success 래핑해 반환
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(IllegalStateException("API 응답 본문이 비어있습니다."))
            } else {
                // 응답이 성공적이지 않으면 HTTP 에러를 Result.failure 반환함
                Result.failure(RuntimeException("게시물 생성 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 네트워크 오류 등 예외 발생 시 Result.failure 반환함.
            Result.failure(e)
        }
    }

    suspend fun searchPostsByTitle(query: String): Result<List<Post>> {
        println("Repository에서 검색 시작: query=$query")
        return try {
            // API 호출
            val response = postApiService.searchPosts(query)

            if(response.isSuccessful) {
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
}