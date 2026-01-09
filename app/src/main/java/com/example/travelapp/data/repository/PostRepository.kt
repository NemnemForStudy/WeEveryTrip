package com.example.travelapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.travelapp.data.api.CommentApiService
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.CreatePostResponse
import com.example.travelapp.data.model.GeoJsonPoint
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.RouteRequest
import com.example.travelapp.data.model.UpdateImageLocationRequest
import com.example.travelapp.data.model.UpdatePostRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.travelapp.data.model.RoutePoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

open class PostRepository @Inject constructor(
    private val postApiService: PostApiService,
    private val commentApiService: CommentApiService,
    @ApplicationContext private val context: Context
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    suspend fun notifyPostChanged() {
        _refreshTrigger.emit(Unit)
    }

    suspend fun createPost(
        category: String,
        title: String,
        content: String,
        tags: List<String>,
        imageUris: List<Uri>,
        // 사진별 GPS/Day/정렬 정보를 서버(post_image)에 저장하기 위한 JSON payload
        // - WriteViewModel에서 "업로드 이미지 순서"와 동일한 순서로 만들어서 넘겨줘야 함
        imageLocationsJson: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        isDomestic: Boolean = true,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): Result<CreatePostResponse> = withContext(Dispatchers.IO) {
        // 🔥 [핵심 1] withContext(Dispatchers.IO)로 감싸서 백그라운드에서 실행 (앱 안 멈춤)
        return@withContext try {
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsBody = tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())
            val isDomesticBody = isDomestic.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // 서버에서 req.body.imageLocations 로 받으므로 Part 이름은 반드시 "imageLocations"
            val imageLocationsBody = imageLocationsJson
                ?.toRequestBody("application/json".toMediaTypeOrNull())

            val parsedList = try {
                imageLocationsJson?.let {
                    Json.decodeFromString<List<RoutePoint>>(it)
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            val finalLat = latitude ?: parsedList.firstOrNull { it.latitude != 0.0 }?.latitude ?: 0.0
            val finalLng = longitude ?: parsedList.firstOrNull { it.longitude != 0.0 }?.longitude ?: 0.0

            val geoPoint = GeoJsonPoint(
                type = "Point",
                coordinates = listOf(finalLng, finalLat) // [경도, 위도] 순서
            )

            val coordinatesBody = Json.encodeToString(geoPoint)
                .toRequestBody("application/json".toMediaTypeOrNull())
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
                        // 2. EXIF orientation 읽어서 회전 적용
                        val rotatedBitmap = try {
                            val exifStream = context.contentResolver.openInputStream(uri)
                            val exif = exifStream?.let { ExifInterface(it) }
                            exifStream?.close()
                            
                            val orientation = exif?.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            ) ?: ExifInterface.ORIENTATION_NORMAL
                            
                            val matrix = Matrix()
                            when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                                ExifInterface.ORIENTATION_TRANSPOSE -> {
                                    matrix.postRotate(90f)
                                    matrix.preScale(-1f, 1f)
                                }
                                ExifInterface.ORIENTATION_TRANSVERSE -> {
                                    matrix.postRotate(270f)
                                    matrix.preScale(-1f, 1f)
                                }
                            }
                            
                            if (orientation != ExifInterface.ORIENTATION_NORMAL && 
                                orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            } else {
                                bitmap
                            }
                        } catch (e: Exception) {
                            Log.w("PostRepository", "EXIF 읽기 실패, 원본 사용: ${e.message}")
                            bitmap
                        }
                        
                        // 3. 압축해서 임시 파일로 저장 (Quality 70%)
                        val file = File(context.cacheDir, "resized_${UUID.randomUUID()}.jpg")
                        val outputStream = FileOutputStream(file)
                        // 압축 및 임시 파일 저장 - compress, 품질 70%
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        outputStream.flush()
                        outputStream.close()
                        
                        // 메모리 해제 (원본과 회전본이 다른 경우만)
                        if (rotatedBitmap !== bitmap) {
                            bitmap.recycle()
                        }

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

            val startDateBody = startDateMillis?.toString()
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val endDateBody = endDateMillis?.toString()
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = postApiService.createPost(
                category = categoryBody,
                title = titleBody,
                content = contentBody,
                tags = tagsBody,
                images = imageParts,
                coordinates = coordinatesBody,
                isDomestic = isDomesticBody,
                // 사진별 좌표 메타(없으면 null로 보내서 서버에서 그냥 빈 배열로 처리)
                imageLocations = imageLocationsBody,
                startDate = startDateBody,
                endDate = endDateBody
            )

            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        Result.success(apiResponse.data)
                    } else {
                        Result.failure(IllegalStateException("게시물 생성 실패"))
                    }
                } ?: Result.failure(IllegalStateException("API 응답 본문이 비어있습니다."))
            } else {
                Result.failure(RuntimeException("게시물 생성 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updatePost(
        postId: String, // 어떤 게시물 수정할지 Id 필요
        category: String? = null,
        title: String? = null,
        content: String? = null,
        tags: List<String>? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        isDomestic: Boolean? = null,
        travelStartDate: String? = null,
        travelEndDate: String? = null,
        images: List<String>? = null,
        imageLocations: List<UpdateImageLocationRequest>? = null
    ): Result<Post> = withContext(Dispatchers.IO) {
        try {
            // 좌표 정보 생성
            val coordinate = if(longitude != null && latitude != null) {
                GeoJsonPoint(
                    type = "Point",
                    coordinates = listOf(longitude, latitude)
                )
            } else {
                null
            }

            // 서버에 보낼 Request
            val request = UpdatePostRequest(
                category = category,
                title = title,
                content = content,
                tags = tags,
                coordinate = coordinate,
                locationName = locationName,
                isDomestic = isDomestic,
                travelStartDate = travelStartDate,
                travelEndDate = travelEndDate,
                images = images,
                imageLocations = imageLocations
            )

            val response = postApiService.updatePost(postId, request)

            if(response.isSuccessful) {
                val body = response.body()
                if(body != null && body.success) {
                    Result.success(body.data!!)
                } else {
                    Result.failure(Exception(body?.message ?: "게시물 수정 실패"))
                }
            } else {
                Result.failure(Exception("서버 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = postApiService.deletePost(postId)
            if(response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("삭제 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
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

    /**
     * 길찾기 API 함수 호출
     * @param locations: 그날 방문한 사진들의 좌표 목록
     * @return: 실제 도로 경로를 구성하는 좌표 목록 (실패 시 null)
     */

    open suspend fun getRouteForDay(locations: List<RoutePoint>): List<RoutePoint>? {
        return try {
            // 1. 요청 객체 생성 (DTO로 감싸기)
            val request = RouteRequest(locations)

            // 2. Retrofit으로 API 호출
            // (AuthInterceptor가 연결되어 있다면 토큰도 알아서 붙어서 나갑니다 👍)
            val response = postApiService.getRouteForDay(request)

            // 3. 응답 처리
            if (response.isSuccessful) {
                // 성공 시: 응답 본문(body)에서 route 리스트를 꺼내 반환
                val body = response.body()
                Log.d("PostRepository", "route success body=${body}")
                body?.route
            } else {
                val err = response.errorBody()?.string()
                Log.e("PostRepository", "route fail code=${response.code()} msg=${response.message()} errBody=$err")
                null
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "route exception=${e.message}", e)
            null
        }
    }

    /**
     * 게시물 좋아요 요청
     * @param postId 게시물 ID
     * @return Result<Unit> 성공하면 Unit, 실패하면 Exception 포함
     */
    suspend fun likePost(postId: String): Result<Unit> {
        return try {
            // Retrofit API 호출(IO 스레드 처리는 내부적으로 해줌)
            val response = postApiService.likePost(postId)

            // 상태 코드 확인
            if(response.isSuccessful) {
                // 성공 시 백엔드에서 준 body 확인
                // body가 null 일 수도 있으니 body()?.let { ... } 처리
                val body = response.body()

                if(body != null && body.success) { //body.success는 ApiResponse의 필드라고 가정
                    Result.success(Unit)
                } else {
                    // HTTP는 200인데 로직상 실패인 경우.
                    Result.failure(Exception(body?.message ?: "알 수 없는 서버"))
                }
            } else {
                Result.failure(Exception("네트워크 요청 실패: 코드 ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLikeCount(postId: String): Result<Int> {
        return try {
            val response = postApiService.getLikeCount(postId)

            if(response.isSuccessful) {
                val body = response.body()

                if(body != null && body.success) {
                    // val data: T? <- 이렇게 되어있음
                    // 그래서 Result.success(Int) 이게 아니라 body에서 int를 꺼내줘야함
                    Result.success(body.data ?: 0)
                } else {
                    Result.failure(Exception(body?.message ?: "알 수 없는 서버"))
                }
            } else {
                Result.failure(Exception("네트워크 요청 실패: 코드 ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unLikePost(postId: String): Result<Unit> {
        return try {
            val response = postApiService.unlikePost(postId)

            if(response.isSuccessful) {
                val body = response.body()

                // Body가 null이거나 (Content-Length: 0), 비즈니스 로직이 실패했을 때
                if(body == null) {
                    // HTTP 204 No Content처럼 Body 없이 성공했으나, 명시적 처리가 필요한 경우
                    // 여기서는 API 응답 계약상 Body가 필수라고 가정하고 실패로 처리합니다.
                    return Result.failure(IllegalStateException("서버 응답 본문이 비어있습니다."))
                }

                if(body.success) {
                    // 비즈니스 로직 성공
                    return Result.success(Unit)
                } else {
                    // Http 200 인데 로직상 실패
                    Result.failure(Exception(body?.message ?: "알 수 없는 서버"))
                }
            } else {
                Result.failure(Exception("네트워크 실패 요청 코드 ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isPostLiked(postId: String): Result<Boolean> {
        return try {
            val response = postApiService.isPostLiked(postId)

            if(response.isSuccessful) {
                val body = response.body()

                if(body != null && body.success) {
                    Result.success(body.data ?: false)
                } else {
                    Result.failure(Exception(body?.message ?: "알 수 없는 소보"))
                }
            } else {
                Result.failure(Exception("네트워크 실패 요청 코드 ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String, isCurrentlyLiked: Boolean): Result<Unit> {
        return if(isCurrentlyLiked) {
            // 현재 좋아요 상태(true) -> false
            unLikePost(postId)
        } else {
            // 현재 좋아요 상태가 아님(false) -> 좋아요 API 호출
            likePost(postId)
        }
    }
}