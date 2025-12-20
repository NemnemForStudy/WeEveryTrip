package com.example.travelapp.data.repository

import android.content.Context
import com.example.travelapp.data.api.CommentApiService
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.ApiResponse
import com.example.travelapp.data.model.CreatePostResponse
import com.example.travelapp.data.model.Post
import com.example.travelapp.data.model.UpdatePostRequest
import com.example.travelapp.data.model.UpdatePostResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
// ⭐️ [변경] JUnit 4용 Import 사용 (jupiter 아님)
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * PostRepository의 단위 테스트 클래스 (JUnit 4 버전)
 * * 주의: androidTest 폴더에 있으므로 JUnit 4를 사용해야 합니다.
 */
class PostRepositoryTest {

    @Mock
    private lateinit var mockPostApiService: PostApiService
    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockCommentApiService: CommentApiService

    private lateinit var postRepository: PostRepository

    private val samplePost = Post(
        id = "test-id-123",
        category = "여행",
        title = "제주도 여행 후기",
        content = "제주도의 푸른 밤",
        nickname = "테스터",
        created_at = "2025-11-28",
        tags = listOf("제주도", "여행", "맛집"),
        imgUrl = "https://example.com/image.jpg"
    )

    // ⭐️ [변경] @BeforeEach -> @Before (JUnit 4)
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        postRepository = PostRepository(mockPostApiService, mockCommentApiService, mockContext)
    }

    @Test
    fun testSearchPostSearch() = runTest {
        val query = "제주도"
        val expectedPosts = listOf(samplePost)
        val mockResponse = Response.success(expectedPosts)

        whenever(mockPostApiService.searchPosts(query))
            .thenReturn(mockResponse)

        val result = postRepository.searchPostsByTitle(query)

        // ⭐️ [변경] JUnit 4는 메시지가 맨 앞에 옵니다.
        // assertTrue(message, condition)
        assertTrue("검색 결과는 성공적이여야 합니다.", result.isSuccess)
        // assertEquals(message, expected, actual)
        assertEquals("반환된 게시물 리스트가 예상과 일치해야 합니다.", expectedPosts, result.getOrNull())
    }

    @Test
    fun testSearchPostFailure_NetworkError() = runTest {
        val query = "제주도"
        val expectException = RuntimeException("네트워크 연결 실패")

        whenever(mockPostApiService.searchPosts(query))
            .thenThrow(expectException)

        val result = postRepository.searchPostsByTitle(query)

        assertTrue("검색 실패해야 함", result.isFailure)
        assertEquals("예외가 올바르게 전달되어야 함.", expectException, result.exceptionOrNull())
    }

    @Test
    fun testSearchPostFailure_HttpError() = runTest {
        val query = "존재하지 않는 게시물"
        val mockResponse = Response.error<List<Post>>(
            404,
            okhttp3.ResponseBody.create(null, "Not Found")
        )

        whenever(mockPostApiService.searchPosts(query))
            .thenReturn(mockResponse)

        val result = postRepository.searchPostsByTitle(query)

        assertTrue("검색 실패해야 함", result.isFailure)
        assertTrue("IllegalStateException 발생해야함.", result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun testSearchPostEmptyResult() = runTest {
        val query = "검색결과없음"
        val emptyList = emptyList<Post>()
        val mockResponse = Response.success(emptyList)

        whenever(mockPostApiService.searchPosts(query))
            .thenReturn(mockResponse)

        val result = postRepository.searchPostsByTitle(query)

        assertTrue("검색은 성공이어야 한다.", result.isSuccess)
        assertEquals("빈 리스트 반환", emptyList, result.getOrNull())
    }

    @Test
    fun testGetAllPostsSuccess() = runTest {
        val expectedPosts = listOf(samplePost, samplePost.copy(id = "2", title = "부산 여행"))
        val mockResponse = Response.success(expectedPosts)

        whenever(mockPostApiService.getAllPosts()).thenReturn(mockResponse)

        val result = postRepository.getAllPosts()

        assertTrue("전제 조회 성공", result.isSuccess)
        assertEquals("리스트 일치", expectedPosts, result.getOrNull())
    }

    @Test // ⭐️ 실패 케이스도 추가
    fun testGetAllPostsFailure() = runTest {
        val expectException = RuntimeException("API 서버 다운")
        whenever(mockPostApiService.getAllPosts()).thenThrow(expectException)

        val result = postRepository.getAllPosts()

        assertTrue("전체 조회 실패해야 함", result.isFailure)
        assertEquals("예외 전달 확인", expectException, result.exceptionOrNull())
    }

    @Test
    fun testCreatePostSuccess() = runTest {
        // 1. 테스트용 Post 객체
        // (samplePost 정의는 클래스 필드에 있다고 가정)

        // 🔥 2. [수정] CreatePostResponse 타입의 Mocking 객체를 생성합니다.
        // Post 객체 자체가 CreatePostResponse라고 가정하고 Mocking합니다.
        // 만약 Post가 CreatePostResponse와 필드가 완전히 같다면, as 캐스팅을 사용합니다.
        val expectedResponse: CreatePostResponse = samplePost as CreatePostResponse

        // 3. Mocking을 위해 ApiResponse 껍데기에 CreatePostResponse를 담음
        val mockApiBody = ApiResponse<CreatePostResponse>(
            success = true,
            message = "게시물 생성 완료",
            data = expectedResponse // 👈 타입 일치!
        )

        // 4. Retrofit Response에 담습니다.
        val mockResponse = Response.success(mockApiBody)

        // whenever 구문은 그대로 유지
        whenever(mockPostApiService.createPost(
            any(), any(), any(), any(), anyOrNull(),
            any(), any(), any(), any(), any()
        )).thenReturn(mockResponse) // 이제 Argument type mismatch 에러가 사라집니다.

        // 5. Repository 호출
        val result = postRepository.createPost(
            category = "여행",
            title = "제주도 여행 후기!",
            content = "너무 좋았어",
            tags = listOf("제주도", "휴가", "힐링"),
            imageUris = emptyList()
        )

        // 6. 검증: Repository는 Result<Post>를 반환해야 하므로, samplePost와 비교
        assertTrue("게시물 생성은 성공", result.isSuccess)
        assertEquals("생성된 게시물 반환", samplePost, result.getOrNull())
    }

    @Test
    fun testCreatePostFailure() = runTest {
        val expectException = RuntimeException("서버 연결 실패")

        whenever(
            mockPostApiService.createPost(
                any(),                       // title: RequestBody
                any(),                       // content
                any(),                       // category
                anyOrNull(),                 // coordinates (nullable)
                any(),                       // isDomestic
                anyOrNull(),                 // imageLocations
                anyOrNull(),                 // tags
                any<List<MultipartBody.Part>>(), // images
                anyOrNull(),                 // startDate
                anyOrNull()                  // endDate
            )
        ).thenThrow(expectException)

        val result = postRepository.createPost(
            category = "여행",
            title = "테스트",
            content = "테스트 내용",
            tags = listOf("테스트"),
            imageUris = emptyList()
        )

        assertTrue("게시물 생성 실패", result.isFailure)
    }

    @Test
    fun testLikePostSuccess() = runTest {
        val postId = "test-post-123"

        val mockApiResponse = ApiResponse<Unit>(
            success = true,
            message = "좋아요 성공",
            data = null
        )

        val mockResponse = Response.success(mockApiResponse)

        whenever(mockPostApiService.likePost(postId))
            .thenReturn(mockResponse)

        val result = postRepository.likePost(postId)

        assertTrue("좋아요 성공", result.isSuccess)
    }

    @Test
    fun testLikePostFailure_BusinessLogic() = runTest {
        val postId = "test-post-123"
        val failMessage = "이미 좋아요를 누른 게시물입니다."

        val mockApiResponse = ApiResponse<Unit>(
            success = false,
            message = failMessage,
            data = null
        )

        val mockResponse = Response.success(mockApiResponse)

        whenever(mockPostApiService.likePost(postId))
            .thenReturn(mockResponse)

        val result = postRepository.likePost(postId)

        assertTrue("비즈니스 로직 실패 시 결과는 Failure", result.isFailure)
        assertEquals("서버 에러 메시지가 전달되어야 함.", failMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun testLikePostFailure_NetworkError() = runTest {
        val postId = "test-post-123"
        val expectedException = RuntimeException("네트워크 연결 끊김")

        whenever(mockPostApiService.likePost(postId))
            .thenThrow(expectedException)

        val result = postRepository.likePost(postId)

        assertTrue("네트워크 오류 시 실패", result.isFailure)
        assertEquals("발생한 예외 전달", expectedException, result.exceptionOrNull())
    }

    @Test
    fun unLikePostSuccess() = runTest {
        val postId = "test-post-123"

        val mockApiResponse = ApiResponse<Unit>(
            success = true,
            message = "좋아요 취소 성공",
            data = null
        )

        val mockResponse = Response.success(mockApiResponse)
        whenever(mockPostApiService.unlikePost(postId))
            .thenReturn(mockResponse)
        val result = postRepository.unLikePost(postId)

        assertTrue("좋아요 취소 성공", result.isSuccess)
        assertEquals("좋아요 취소 되어야함.", Unit, result.getOrNull())
    }

    @Test
    fun unLikePostFailure_BusinessLogic() = runTest {
        val postId = "test-post-123"
        val failMessage = "이미 취소된 게시물입니다."

        val mockApiResponse = ApiResponse<Unit>(
            success = false,
            message = failMessage,
            data = null
        )

        val mockResponse = Response.success(mockApiResponse)

        whenever(mockPostApiService.unlikePost(postId))
            .thenReturn(mockResponse)
        val result = postRepository.unLikePost(postId)
        assertTrue("비즈니스 로직 실패", result.isFailure)
        assertEquals("서버 에러 메시지 전달", failMessage, result.exceptionOrNull()?.message)
    }

    @Test
    // null 처리 검증
    fun unLikeFailure_Null() = runTest {
        val postId = "test-post-123"

        val mockResponse: Response<ApiResponse<Unit>> = Response.success(null)

        whenever(mockPostApiService.unlikePost(postId))
            .thenReturn(mockResponse)

        val result = postRepository.unLikePost(postId)

        assertTrue("바디가 널일 때, Repository에서 실패로 처리해야 합니다.", result.isFailure)
    }

    @Test
    fun unLikePostFailure_NetworkError() = runTest {
        val postId = "test-post-123"
        val expectedException = RuntimeException("네트워크 연결 끊김")

        whenever(mockPostApiService.unlikePost(postId))
            .thenThrow(expectedException)

        val result = postRepository.unLikePost(postId)
        assertTrue("네트워크 오류", result.isFailure)
        assertEquals("발생 예외 전달", expectedException, result.exceptionOrNull())
    }

    @Test
    fun testGetLikeCountSuccess() = runTest {
        val postId = "test-post-123"
        val expectCount = 42

        // 백엔드 응답 구조 흉내내기
        val mockApiResponse = ApiResponse(
            success = true,
            message = "조회 성공",
            data = expectCount
        )
        val mockResponse = Response.success(mockApiResponse)

        // API 호출되면 위에서 만든 가짜 응답 리턴하도록 설정
        whenever(mockPostApiService.getLikeCount(postId))
            .thenReturn(mockResponse)

        // when (실행)
        val result = postRepository.getLikeCount(postId)

        // Then 검증
        assertTrue("좋아요 개수 조회는 성공해야 합니다.", result.isSuccess)
        assertEquals("반환된 개수가 예상값(42)과 일치해야 합니다,", expectCount, result.getOrNull())
    }

    @Test
    fun testGetLikeCountFailure_NetworkError() = runTest {
        val postId = "test-post-123"
        val expectedException = RuntimeException("네트워크 연결 끊김")

        whenever(mockPostApiService.getLikeCount(postId))
            .thenThrow(expectedException)

        val result = postRepository.getLikeCount(postId)

        assertTrue("네트워크 오류 시 실패로 처리되어야 함.", result.isFailure)
        assertEquals("발생한 예외가 그래도 전달되어야 한다.", expectedException, result.exceptionOrNull())
    }

    @Test
    fun testIsPostLikedSuccess() = runTest {
        val postId = "test-post-123"

        val mockApiResponse = ApiResponse<Boolean>(
            success = true,
            message = "상태 조회 성공",
            data = true
        )

        val mockResponse = Response.success(mockApiResponse)
        whenever(mockPostApiService.isPostLiked(postId))
            .thenReturn(mockResponse)

        val result = postRepository.isPostLiked(postId)

        assertTrue("상태 조회 성공", result.isSuccess)
        assertEquals("성공 전달", true, result.getOrNull())
    }

    @Test
    fun testIsPostLikedSuccess_NotLiked() = runTest {
        val postId = "test-post-123"

        val mockApiResponse = ApiResponse<Boolean>(
            success = true,
            message = "조회 성공",
            data = false
        )

        val mockResponse = Response.success(mockApiResponse)

        whenever(mockPostApiService.isPostLiked(postId))
            .thenReturn(mockResponse)

        val result = postRepository.isPostLiked(postId)
        assertTrue("조회 실패", result.isSuccess)
        assertEquals("서버 에러 메시지 전달", false, result.getOrNull())
    }

    @Test
    fun testIsPostLikedFailure() = runTest {
        val postId = "test-post-123"
        val failMessage = "권한이 없습니다."

        val mockApiResponse = ApiResponse<Boolean>(
            success = false,
            message = failMessage,
            data = null
        )

        val mockResponse = Response.success(mockApiResponse)

        whenever(mockPostApiService.isPostLiked(postId)).thenReturn(mockResponse)

        val result = postRepository.isPostLiked(postId)

        assertTrue("조회 실패", result.isFailure)
        assertEquals("서버 에러 메시지 전달", failMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun testIdPostIdLike_NetworkError() = runTest {
        val postId = "test-post-123"
        val expectedException = RuntimeException("네트워크 연결 끊김")

        whenever(mockPostApiService.unlikePost(postId))
            .thenThrow(expectedException)

        val result = postRepository.unLikePost(postId)
        assertTrue("네트워크 오류", result.isFailure)
        assertEquals("발생 예외 전달", expectedException, result.exceptionOrNull())
    }

    @Test
    fun testUpdatePost_Success() = runTest {
        val postId = "test"

        val mockApiBody = ApiResponse<Post>(
            success = true,
            message = "게시물 수정 완료",
            data = samplePost
        )

        val mockResponse = Response.success(mockApiBody)

        whenever(mockPostApiService.updatePost(eq(postId), any()))
            .thenReturn(mockResponse)

        val result = postRepository.updatePost(
            postId = postId,
            category = "1",
            title = "수정된 제목",
            content = "수정된 내용"
        )

        // 5. 검증
        assertTrue("게시물 수정 성공", result.isSuccess)
        assertEquals(samplePost, result.getOrNull())
    }

    @Test
    fun testDeleteSuccess() = runTest {
        val postId = "test-post-123"
        val mockApiResponse = ApiResponse<Unit>(
            success = true,
            message = "게시물 삭제 성공",
            data = null
        )

        val mockResponse = Response.success(mockApiResponse)
        whenever(mockPostApiService.deletePost(postId))
            .thenReturn(mockResponse)

        val result = postRepository.deletePost(postId)
        assertTrue("게시물 삭제 성공", result.isSuccess)
        assertEquals("결과는 Unit이어야 함", Unit, result.getOrNull())
    }

    @Test
    fun testDeleteFailure() = runTest {
        val postId = "test-post-123"
        val errorMessage = "Server error"

        val errorResponse = Response.error<ApiResponse<Unit>>(
            500,
            errorMessage.toResponseBody("application/json".toMediaType())
        )
        whenever(mockPostApiService.deletePost(postId))
            .thenReturn(errorResponse)

        val result = postRepository.deletePost(postId)

        // 결과가 실패인지 확인
        assertTrue("게시물 삭제는 실패로 처리되어야 합니다.", result.isFailure)

        val exception = result.exceptionOrNull()
        assertNotNull("실패 시 예외 객체가 Result에 포함되어야 합니다.", exception)

        // 타입 비교 시 실제 발생한 타입을 메시지에 포함하면 디버깅이 훨씬 쉬워집니다.
        assertTrue(
            "실제 발생한 예외 타입: ${exception?.javaClass?.simpleName}",
            exception is RuntimeException
        )
    }
}