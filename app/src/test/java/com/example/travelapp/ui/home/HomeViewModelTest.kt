package com.example.travelapp.ui.home

import com.example.travelapp.data.model.Post
import com.example.travelapp.data.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
// ⭐️ 중요: JUnit 5의 Assertions를 사용해야 합니다.
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel
    private lateinit var mockPostRepository: PostRepository
    private val testDispatcher = StandardTestDispatcher()

    // 각 @Test 함수 실행되기 전 항상 먼저 실행되는 함수
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Mockito를 사용하여 가짜 객체 생성
        mockPostRepository = mock()
        // ViewModel에 가짜 Repository 주입
        viewModel = HomeViewModel(mockPostRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 실패하는 테스트 (TDD Red Phase)
    @Test
    fun `performSearch_should_update_searchResults_on_success`() = runTest {
        // Given (준비)
        val searchQuery = "테스트"
        val fakeSearchResults = listOf(
            Post(
                id = "1",
                category = "1",
                title = "테스트 제목 1",
                content = "내용 1",
                nickname = "유저 1",
                created_at = "2025-11-27",
                tags = emptyList(),
                imgUrl = null
            ),
            Post(
                id = "2",
                category = "2",
                title = "테스트 제목 2",
                content = "내용 2",
                nickname = "유저 2",
                created_at = "2025-11-27",
                tags = listOf("태그 1", "태그 2"),
                imgUrl = "http://example.com/image.jpg"
            )
        )

        // searchPostsByTitle 호출되면 성공으로 fakeSearchResults 반환하도록 설정 (Stubbing)
        whenever(mockPostRepository.searchPostsByTitle(searchQuery)).thenReturn(Result.success(fakeSearchResults))

        // When (실행)
        viewModel.performSearch(searchQuery)

        // Then (검증)
        testDispatcher.scheduler.advanceUntilIdle() // 예약된 코루틴 작업 모두 실행
        val searchResult = viewModel.searchResults.first() // searchResults StateFlow 첫 번째 값을 가져옴

        // JUnit 5 assertEquals 사용
        assertEquals(2, searchResult.size)
        assertEquals("테스트 제목 1", searchResult[0].title)
    }
}