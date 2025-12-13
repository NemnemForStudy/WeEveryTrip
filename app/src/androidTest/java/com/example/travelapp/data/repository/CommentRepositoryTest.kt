package com.example.travelapp.data.repository

import android.content.Context
import com.example.travelapp.data.api.CommentApiService
import com.example.travelapp.data.model.ApiResponse
import com.example.travelapp.data.model.comment.Comment
import com.example.travelapp.data.model.comment.CreateCommentRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import retrofit2.Response

class CommentRepositoryTest {

    @Mock
    private lateinit var mockCommentApiService: CommentApiService

    @Mock
    private lateinit var mockContext: Context

    private lateinit var commentRepository: CommentRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        commentRepository = CommentRepository(mockCommentApiService, mockContext)
    }

    @Test
    fun testCreateCommentSuccess() = runTest {
        val postId = "53"
        val content = "테스트 댓글입니다."

        val mockComment = Comment(
            commentId = "100",
            postId = postId,
            userId = "user1",
            content = content,
            createdAt = "2025-12-12",
            updatedAt = "2025-12-12",
            nickname = "테스트유저",
            profileImage = null
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "성공!",
            data = mockComment
        )

        val mockResponse = Response.success(apiResponse)

        whenever(mockCommentApiService.createComment(postId, CreateCommentRequest(content)))
            .thenReturn(mockResponse)

        val result = commentRepository.createComment(postId, content)

        assertTrue("comment 생성 성공", result.isSuccess)
        assertEquals("성공 전달", mockComment, result.getOrNull())
    }

    @Test
    fun testCreateCommentFailure() = runTest {
        val postId = "53"
        val content = "금지된 단어 포함"
        val failMessage = "욕설 포함"

        val mockApiResponse = ApiResponse<Comment>(
            success = false,
            message = failMessage,
            data = null
        )

        val mockResponse = Response.success(mockApiResponse)

        whenever(mockCommentApiService.createComment(postId, CreateCommentRequest(content)))
            .thenReturn(mockResponse)

        val result = commentRepository.createComment(postId, content)

        assertTrue("comment 생성 실패", result.isFailure)
        assertEquals("실패 전달", failMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun testCreateCommentNetworkError() = runTest {
        val postId = "53"
        val content = "아무 내용"
        val expectedException = RuntimeException("네트워크 연결 오류")

        whenever(mockCommentApiService.createComment(postId, CreateCommentRequest(content)))
            .thenThrow(expectedException)

        val result = commentRepository.createComment(postId, content)

        assertTrue("comment 생성 실패", result.isFailure)
        assertEquals("네트워크 연결 오류",  result.exceptionOrNull()?.message)
    }
}