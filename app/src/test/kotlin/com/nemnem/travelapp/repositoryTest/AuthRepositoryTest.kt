package com.nemnem.travelapp.repositoryTest

import com.nemnem.travelapp.data.api.AuthApiService
import com.nemnem.travelapp.data.model.SocialLoginResponse
import com.nemnem.travelapp.data.model.User
import com.nemnem.travelapp.data.repository.AuthRepository
import com.nemnem.travelapp.util.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private val mockAuthApiService = mockk<AuthApiService>(relaxed = true)
    private val mockTokenManager = mockk<TokenManager>(relaxed = true)

    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        authRepository = AuthRepository(mockAuthApiService, mockTokenManager)
    }

    @Test
    fun `소셜 로그인 성공`() = runTest {
        val mockUser = mockk<User>(relaxed = true)  // ← 이거 추가!

        val mockResponse = SocialLoginResponse(
            message = "로그인 성공",
            token = "test_token",
            user = mockUser
        )
        coEvery {
            mockAuthApiService.socialLogin(any())
        } returns Response.success(mockResponse)

        val result = authRepository.socialLogin(
            provider = "GOOGLE",
            token = "google_token",
            email = "test@test.com",
            socialId = "12345"
        )

        assertTrue(result.isSuccess)
        coVerify { mockTokenManager.saveToken("test_token") }
    }

    @Test
    fun `로그아웃 성공`() = runTest {
        coEvery {
            mockAuthApiService.logout()
        } returns Response.success(Unit)

        val result = authRepository.logout()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `프로필 조회 성공`() = runTest {
        val mockUser = mockk<User>(relaxed = true)

        coEvery {
            mockAuthApiService.getMyProfile()
        } returns Response.success(mockUser)

        val result = authRepository.getMyProfile()

        assertTrue(result.isSuccess)
        assertEquals(mockUser, result.getOrNull())
    }
}