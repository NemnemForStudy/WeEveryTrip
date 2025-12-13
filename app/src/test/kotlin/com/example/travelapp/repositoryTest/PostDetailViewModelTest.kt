package com.example.travelapp.repositoryTest

import com.example.travelapp.data.api.PostApiService
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.data.repository.CommentRepository
import com.example.travelapp.data.repository.PostRepository
import com.example.travelapp.ui.Detail.PostDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest {
    private val mockPostApiService = mockk<PostApiService>(relaxed = true)
    private val mockPostRepository = mockk<PostRepository>(relaxed = true)
    private val mockCommentRepository = mockk<CommentRepository>(relaxed = true)

    // 테스트할 진짜 객체
    private lateinit var viewModel: PostDetailViewModel

    // 코루틴 테스트용 Dispatcher 설정
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModel에서 viewModelScope 쓰기 때문에 설정 필요
        Dispatchers.setMain(testDispatcher)

        viewModel = PostDetailViewModel(
            postApiService = mockPostApiService,
            postRepository = mockPostRepository,
            commentRepository = mockCommentRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `댓글 작성 성공 시 입력창이 비워지고 목록을 새로고침 한다`() = runTest {
        val postId = "53"
        val content = "테스트 댓글"

        coEvery {
            mockCommentRepository.createComment(postId, content)
        } returns Result.success(mockk())

        coEvery {
            mockCommentRepository.getComments(postId)
        } returns Result.success(emptyList()) // 빈 리스트 리턴

        viewModel.updateComment(content)
        viewModel.createComment(postId)

        testDispatcher.scheduler.advanceUntilIdle()

        // 입력창이 비워졌는가?
        assertEquals("", viewModel.commentContent.value)
        // createComment가 실제 호출?
        coVerify {
            mockCommentRepository.createComment(postId, content)
        }

        // 목록 새로고침 호출 되었는가?
        coVerify { mockCommentRepository.getComments(postId) }
    }

    @Test
    fun `댓글 목록 로드 성공 시 comments 상태가 업데이트 된다`() = runTest {
        val postId = "53"
        val mockList = listOf(
            Comment("1", postId, "user1", "댓글1", "date", "date", "닉네임1"),
            Comment("2", postId, "user2", "댓글2", "date", "date", "닉네임2")
        )

        // 목록 달라고하면 mockList를 줘
        coEvery {
            mockCommentRepository.getComments(postId)
        } returns Result.success(mockList)

        viewModel.loadComments(postId)
        testDispatcher.scheduler.advanceUntilIdle()

        // ViewModel의 상태(StateFlow)가 mockList로 잘 변했는지 확인
        assertEquals(2, viewModel.comments.value.size)
        assertEquals("댓글1", viewModel.comments.value[0].content)
    }
}

