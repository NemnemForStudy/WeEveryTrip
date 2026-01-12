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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // ✅ 싱글톤으로 변경 (앱 전체에서 하나의 인스턴스만 사용)
open class PostRepository @Inject constructor(
    private val postApiService: PostApiService,
    private val commentApiService: CommentApiService,
    @ApplicationContext private val context: Context
) {
    // ✅ 전역 새로고침 트리거 추가
    private val _shouldRefreshAll = MutableStateFlow(0L)
    val shouldRefreshAll: StateFlow<Long> = _shouldRefreshAll.asStateFlow()

    // ✅ 게시물 캐시
    private var cachedPosts: List<Post>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_VALIDITY = 5 * 60 * 1000L // 5분

    /**
     * 전역 새로고침 트리거 발동
     */
    private fun triggerGlobalRefresh() {
        Log.d("PostRepository", "🔔 전역 새로고침 트리거 발동")
        _shouldRefreshAll.value = System.currentTimeMillis()
    }

    /**
     * 캐시 무효화
     */
    private fun invalidateCache() {
        Log.d("PostRepository", "🗑️ 캐시 무효화")
        cachedPosts = null
        cacheTimestamp = 0
    }

    suspend fun createPost(
        category: String,
        title: String,
        content: String,
        tags: List<String>,
        imageUris: List<Uri>,
        imageLocationsJson: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        isDomestic: Boolean = true,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): Result<CreatePostResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsBody = tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())
            val isDomesticBody = isDomestic.toString().toRequestBody("text/plain".toMediaTypeOrNull())

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
                coordinates = listOf(finalLng, finalLat)
            )

            val coordinatesBody = Json.encodeToString(geoPoint)
                .toRequestBody("application/json".toMediaTypeOrNull())

            val imageParts = imageUris.mapNotNull { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    val scale = calculateInSampleSize(options, 1024, 1024)
                    val options2 = BitmapFactory.Options()
                    options2.inSampleSize = scale
                    val realInputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(realInputStream, null, options2)
                    realInputStream?.close()

                    if (bitmap != null) {
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

                        val file = File(context.cacheDir, "resized_${UUID.randomUUID()}.jpg")
                        val outputStream = FileOutputStream(file)
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        outputStream.flush()
                        outputStream.close()

                        if (rotatedBitmap !== bitmap) {
                            bitmap.recycle()
                        }

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
                imageLocations = imageLocationsBody,
                startDate = startDateBody,
                endDate = endDateBody
            )

            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        // ✅ 게시물 생성 성공 시 캐시 무효화 및 전역 새로고침
                        invalidateCache()
                        triggerGlobalRefresh()
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
        postId: String,
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
            val coordinate = if(longitude != null && latitude != null) {
                GeoJsonPoint(
                    type = "Point",
                    coordinates = listOf(longitude, latitude)
                )
            } else {
                null
            }

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
                    // ✅ 수정 성공 시 캐시 무효화 및 전역 새로고침
                    invalidateCache()
                    triggerGlobalRefresh()
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
                // ✅ 삭제 성공 시 캐시 무효화 및 전역 새로고침
                invalidateCache()
                triggerGlobalRefresh()
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

    suspend fun getAllPosts(forceRefresh: Boolean = false): Result<List<Post>> {
        // ✅ 캐시 로직 추가
        if (!forceRefresh && cachedPosts != null &&
            System.currentTimeMillis() - cacheTimestamp < CACHE_VALIDITY) {
            Log.d("PostRepository", "📦 캐시된 데이터 반환 (${cachedPosts!!.size}개)")
            return Result.success(cachedPosts!!)
        }

        Log.d("PostRepository", "🌐 서버에서 게시물 조회")
        return try {
            val response = postApiService.getAllPosts()

            if (!response.isSuccessful) {
                val errorMsg = "게시물 목록을 가져오는데 실패했습니다. (${response.code()}: ${response.message()})"
                Log.e("PostRepository", errorMsg)
                return Result.failure(IOException(errorMsg))
            }

            response.body()?.let { posts ->
                Log.d("PostRepository", "✅ ${posts.size}개의 게시물을 불러왔습니다.")
                // ✅ 캐시에 저장
                cachedPosts = posts
                cacheTimestamp = System.currentTimeMillis()
                Result.success(posts)
            } ?: run {
                Log.w("PostRepository", "⚠️ 게시물이 없거나 응답 형식이 올바르지 않습니다.")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            val errorMsg = "게시물 목록을 불러오는 중 오류가 발생했습니다: ${e.message}"
            Log.e("PostRepository", errorMsg)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    open suspend fun getRouteForDay(locations: List<RoutePoint>): List<RoutePoint>? {
        return try {
            val request = RouteRequest(locations)
            val response = postApiService.getRouteForDay(request)

            if (response.isSuccessful) {
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

    suspend fun likePost(postId: String): Result<Unit> {
        return try {
            val response = postApiService.likePost(postId)

            if(response.isSuccessful) {
                val body = response.body()

                if(body != null && body.success) {
                    Result.success(Unit)
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

    suspend fun getLikeCount(postId: String): Result<Int> {
        return try {
            val response = postApiService.getLikeCount(postId)

            if(response.isSuccessful) {
                val body = response.body()

                if(body != null && body.success) {
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

                if(body == null) {
                    return Result.failure(IllegalStateException("서버 응답 본문이 비어있습니다."))
                }

                if(body.success) {
                    return Result.success(Unit)
                } else {
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
            unLikePost(postId)
        } else {
            likePost(postId)
        }
    }
}