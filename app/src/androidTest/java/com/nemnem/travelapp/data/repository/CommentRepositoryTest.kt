package com.nemnem.travelapp.data.repository

import android.content.Context
import com.nemnem.travelapp.data.api.CommentApiService
import com.nemnem.travelapp.data.model.ApiResponse
import com.nemnem.travelapp.data.model.comment.Comment
import com.nemnem.travelapp.data.model.comment.CreateCommentRequest
import com.nemnem.travelapp.data.model.comment.UpdateCommentRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
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
        commentRepository = CommentRepository(mockCommentApiService)
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

    @Test
    fun testGetCommentSuccess() = runTest {
        val postId = "53"
        val mockComments = listOf(
            Comment(
                commentId = "1",
                postId = postId,
                userId = "user1",
                content = "첫번째 댓글",
                createdAt = "2025-12-12",
                updatedAt = "2025-12-12",
                nickname = "유저1",
                profileImage = null
            ),
            Comment(
                commentId = "2",
                postId = postId,
                userId = "user2",
                content = "두번째 댓글",
                createdAt = "2025-12-12",
                updatedAt = "2025-12-12",
                nickname = "유저2",
                profileImage = null
            )
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "조회 성공",
            data = mockComments
        )

        val mockResponse = Response.success(apiResponse)
        whenever(mockCommentApiService.getComments(postId))
            .thenReturn(mockResponse)

        val result = commentRepository.getComments(postId)

        assertTrue("댓글 목록 조회 성공", result.isSuccess)
        assertEquals("댓글 2개 반환", 2, result.getOrNull()?.size)
    }

    @Test
    fun testUpdateCommentSuccess() = runTest {
        val commentId = "100"
        val newContent = "수정된 댓글 내용"

        val apiResponse = ApiResponse<Comment>(
            success = true,
            message = "댓글 수정 성공",
            data = null
        )

        val mockResponse = Response.success(apiResponse)

        whenever(mockCommentApiService.updateComment(commentId, UpdateCommentRequest(newContent)))
            .thenReturn(mockResponse)

        val result = commentRepository.updateComment(commentId, newContent)

        assertTrue("댓글 수정 성공", result.isSuccess)
        assertEquals("수정 성공 메시지", newContent, result.getOrNull())
    }

    @Test
    fun testUpdateCommentFailure() = runTest {
        val commentId = "100"
        val newContent = "수정된 내용"

        val apiResponse = ApiResponse<Comment>(
            success = false,
            message = "수정 실패",
            data = null
        )

        val mockResponse = Response.success(apiResponse)

        whenever(mockCommentApiService.updateComment(commentId, UpdateCommentRequest(newContent)))
            .thenReturn(mockResponse)

        val result = commentRepository.updateComment(commentId, newContent)

        assertTrue("댓글 수정 실패", result.isFailure)
    }

    @Test
    fun testDeleteCommentSuccess() = runTest {
        val commentId = "100"

        val apiResponse = ApiResponse<Unit>(
            success = true,
            message = "삭제 성공",
            data = null
        )

        val mockResponse = Response.success(apiResponse)

        whenever(mockCommentApiService.deleteComment(commentId))
            .thenReturn(mockResponse)

        val result = commentRepository.deleteComment(commentId)

        assertTrue("댓글 삭제 성공", result.isSuccess)  // ← 이거 추가!
        assertEquals("삭제 성공 메시지", "삭제 성공", result.getOrNull())  // ← 이거 추가!
    }
}