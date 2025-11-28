package com.example.travelapp.data.repository

import android.content.Context
import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.Post
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * PostRepository의 단위 테스트 클래스
 *
 * TDD(Test Driven Development) 원칙에 따라 작성됨:
 * 1. Red: 실패하는 테스트 먼저 작성
 * 2. Green: 테스트를 통과하는 최소한의 코드 작성
 * 3. Refactor: 코드 개선 및 리팩토링
 *
 * 테스트 대상: PostRepository
 * Mock 객체: PostApiService, Context
 */
class PostRepositoryTest {

    // Mock 객체 선언
    // @Mock 어노테이션으로 Mockito가 가짜 객체를 생성하도록 함
    @Mock
    private lateinit var mockPostApiService: PostApiService
    @Mock
    private lateinit var mockContext: Context

    // Mock 객체
    private lateinit var postRepository: PostRepository

    // 테스트용 데이터
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

    /**
     * 각 테스트 실행 전 호출되는 초기화 함수
     *
     * MockitoAnnotations.openMocks(this):
     * - @Mock 어노테이션이 붙은 필드들을 Mock 객체로 초기화
     *
     * PostRepository 인스턴스 생성:
     * - Mock 객체들을 주입하여 실제 네트워크 호출 없이 테스트 가능
     */

    @Before
    fun setUp() {
        // Mock 객체 초기화
        MockitoAnnotations.openMocks(this)
        postRepository = PostRepository(mockPostApiService, mockContext)
    }

    /**
     * 테스트 1: 게시물 검색 성공 시나리오
     *
     * Given : API가 정상적 게시물 리스트 반환할 때
     * When : searchPostsByTitle() 호출
     * Then : Result.success와 함께 리스트 반환
     */

    @Test
    fun `testSearchPostSearch`() = runTest {
        val query = "제주도"
        val expectedPosts = listOf(samplePost)
        val mockResponse = Response.success(expectedPosts)

        // whenever : mockito의 stubbing 메서드
        // searchPosts(query) 호출되면 mockResponse 반환하도록 설정
        whenever(mockPostApiService.searchPosts(query))
            .thenReturn(mockResponse)

        // When : Repository의 검색 함수 호출
        val result = postRepository.searchPostsByTitle(query)

        // Then : 결과 검증
        assertTrue(result.isSuccess, "검색 결과는 성공적이여야 합니다.")
        assertEquals(expectedPosts, result.getOrNull(), "반환된 게시물 리스트가 예상과 일치해야 합니다.")
    }

    /**
     * 테스트 2: 게시물 검색 실패 시나리오 (네트워크 오류)
     *
     * Given : API 호출 시 예외가 발생할 때
     * When : searchPostsByTitle() 호출
     * Then : Result.failure가 반환되어야 함
     */
    @Test
    fun testSearchPostFailure_NetworkError() = runTest {
        val query = "제주도"
        val expectException = RuntimeException("네트워크 연결 실패")

        whenever(mockPostApiService.searchPosts(query))
            .thenThrow(expectException)

        val result = postRepository.searchPostsByTitle(query)

        // Then : 실패 결과 검증
        assertTrue(result.isFailure, "검색 실패")
        assertEquals(expectException, result.exceptionOrNull(), "예외가 올바르게 전달되어야 함.")
    }

    /**
     * 테스트 3: 게시물 검색 실패 시나리오 (HTTP 404 에러)
     *
     * Given : API가 404 에러를 반환할 때
     * When : searchPostsByTitle() 호출
     * Then : Result.failure가 반환되어야 함
     */
    @Test
    fun testSearchPostFailure_HttpError() = runTest {
        val query = "존재하지 않는 게시물"
        val mockResponse = Response.error<List<Post>>(
            404,
            okhttp3.ResponseBody.create(null, "Not Fount")
        )

        whenever(mockPostApiService.searchPosts(query))
            .thenReturn(mockResponse)

        val result = postRepository.searchPostsByTitle(query)

        assertTrue(result.isFailure, "검색 실패")
        assertTrue(result.exceptionOrNull() is IllegalStateException,
            "IllegalStateException 발생해야함.")
    }

    /**
     * 테스트 4: 빈 검색 결과 처리
     *
     * Given : API가 빈 리스트를 반환할 때
     * When : searchPostsByTitle() 호출
     * Then : 빈 리스트와 함께 Result.success가 반환되어야 함
     */
    @Test
    fun testSearchPostEmptyResult() = runTest {
        val query = "검색결과없음"
        val emptyList = emptyList<Post>()
        val mockResponse = Response.success(emptyList)

        whenever(mockPostApiService.searchPosts(query))
            .thenReturn(mockResponse)

        val result = postRepository.searchPostsByTitle(query)

        assertTrue(result.isSuccess, "검색은 성공이어야 한다.")
        assertEquals(emptyList, result.getOrNull(), "빈 리스트 반환")
    }

    /**
     * 테스트 5: 게시물 생성 성공 시나리오
     *
     * Given : API가 정상적으로 게시물을 생성할 때
     * When : createPost() 호출
     * Then : Result.success와 함께 생성된 게시물이 반환되어야 함
     */
    @Test
    fun testCreatePostSuccess() = runTest {
        val mockResponse = Response.success(samplePost)

        // any() 매처를 사용해 어떤 인자가 와도 mockResponse 반환
        whenever(mockPostApiService.createPost(
            any<Map<String, RequestBody>>(),
            any<List<MultipartBody.Part>>()
        )).thenReturn(mockResponse)

        // when: Repo의 게시물 생성 함수
        val result = postRepository.createPost(
            category = "여행",
            title = "제주도 여행 후기!",
            content = "너무 좋았어",
            tags = listOf("제주도", "휴가", "힐링"),
            imageUris = emptyList()
        )

        // Then: 결과 검증
        assertTrue(result.isSuccess, "게시물 생성은 성공")
        assertEquals(samplePost, result.getOrNull(), "생성된 게시물 반환")
    }

    /**
     * 테스트 6: 게시물 생성 실패 시나리오
     *
     * Given : API 호출 시 예외가 발생할 때
     * When : createPost() 호출
     * Then : Result.failure가 반환되어야 함
     */
    @Test
    fun testCreatePostFailure() = runTest {
        val expectException = RuntimeException("서버 연결 실패")

        whenever(
            mockPostApiService.createPost(
                any<Map<String, RequestBody>>(),
                any<List<MultipartBody.Part>>()
            )
        ).thenThrow(expectException)

        val result = postRepository.createPost(
            category = "여행",
            title = "테스트",
            content = "테스트 내용",
            tags = listOf("테스트"),
            imageUris = emptyList()
        )

        // 실패 검증
        assertTrue(result.isFailure, "게시물 생성 실패")
    }
}