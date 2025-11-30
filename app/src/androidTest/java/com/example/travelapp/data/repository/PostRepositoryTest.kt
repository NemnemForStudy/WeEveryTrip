package com.example.travelapp.data.repository

import android.content.Context
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
// ⭐️ [변경] JUnit 4용 Import 사용 (jupiter 아님)
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
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
        postRepository = PostRepository(mockPostApiService, mockContext)
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
        val mockResponse = Response.success(samplePost)

        whenever(mockPostApiService.createPost(
            any<RequestBody>(),
            any<RequestBody>(),
            any<RequestBody>(),
            any<RequestBody>(),
            any<Array<MultipartBody.Part>>()
        )).thenReturn(mockResponse)

        val result = postRepository.createPost(
            category = "여행",
            title = "제주도 여행 후기!",
            content = "너무 좋았어",
            tags = listOf("제주도", "휴가", "힐링"),
            imageUris = emptyList()
        )

        assertTrue("게시물 생성은 성공", result.isSuccess)
        assertEquals("생성된 게시물 반환", samplePost, result.getOrNull())
    }

    @Test
    fun testCreatePostFailure() = runTest {
        val expectException = RuntimeException("서버 연결 실패")

        whenever(
            mockPostApiService.createPost(
                any<RequestBody>(),
                any<RequestBody>(),
                any<RequestBody>(),
                any<RequestBody>(),
                any<Array<MultipartBody.Part>>()
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
}