package com.nemnem.travelapp.ui.home

import com.nemnem.travelapp.data.model.Post
import com.nemnem.travelapp.data.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
// 🔥 [수정 1] Mockito 관련 Import 필수 추가
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel
    private lateinit var mockPostRepository: PostRepository
    private val testDispatcher = StandardTestDispatcher()

    // 🔥 [수정 2] 테스트 데이터를 클래스 멤버 변수로 이동 (setUp에서 쓰기 위해)
    private val fakeSearchResults = listOf(
        Post(
            id = "1", category = "1", title = "테스트 제목 1", content = "내용 1",
            nickname = "유저 1", created_at = "2025-11-27", tags = emptyList(), imgUrl = null
        ),
        Post(
            id = "2", category = "2", title = "테스트 제목 2", content = "내용 2",
            nickname = "유저 2", created_at = "2025-11-27", tags = listOf("태그 1", "태그 2"), imgUrl = "http://example.com/image.jpg"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // 1. Mock 객체 생성
        mockPostRepository = mock()

        // 2. ViewModel 생성 (Mock 주입)
        viewModel = HomeViewModel(mockPostRepository)

        // 🔥 [수정 3] Mocking(Stubbing)을 여기서 미리 실행 (가장 중요!)
        // runTest 블록 밖에서 해야 에러가 안 납니다.
        runBlocking {
            doAnswer {
                Result.success(fakeSearchResults)
            }.`when`(mockPostRepository).searchPostsByTitle(any())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun performSearch_should_update_searchResults_on_success() = runTest {
        // Given (준비)
        val searchQuery = "테스트"
        // Mocking은 이미 setUp에서 완료됨

        // When (실행)
        viewModel.performSearch(searchQuery)

        // Then (검증)
        testDispatcher.scheduler.advanceUntilIdle() // 코루틴 작업 완료 대기

        // StateFlow의 최신 값 가져오기
        val searchResult = viewModel.searchResults.value

        // Assertion 검증
        assertEquals(2, searchResult.size)
        assertEquals("테스트 제목 1", searchResult[0].title)
    }
    
    @Test
    fun Search_Failure() = runTest {
        val search = "테스트"
        val errorMessage = "검색 실패"

        runBlocking {
            doAnswer {
                Result.failure<List<Post>>(Exception(errorMessage))
            }.`when`(mockPostRepository).searchPostsByTitle(search)
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.searchResults.value.isEmpty())
    }
}