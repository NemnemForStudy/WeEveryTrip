package com.nemnem.travelapp.data.repository

import com.nemnem.travelapp.data.api.AuthApiService
import com.nemnem.travelapp.data.model.Setting.NotificationRequest
import com.nemnem.travelapp.data.model.SocialLoginRequest
import com.nemnem.travelapp.data.model.SocialLoginResponse
import com.nemnem.travelapp.data.model.UpdateProfileRequest
import com.nemnem.travelapp.data.model.User
import com.nemnem.travelapp.util.TokenManager
import java.lang.RuntimeException
import javax.inject.Inject

// 인증 관련 처리 담당 클래스
// Hilt 통해 AuthApiService 구현체 주입 받음
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    // ViewModel에서 함수 호출하게 됨.
    // 파라미터로 받은 SocialLoginRequest 객체를 ApiService에 그대로 전달
    // Request(요청서)를 서버로 보내고 Response에 담에서 그대로 되돌려줌.
    suspend fun socialLogin(
        provider: String,
        token: String,
        email: String,
        socialId: String,
        nickname: String?,
        profileImage: String?
    ): Result<SocialLoginResponse> {
        return try {
            val request = SocialLoginRequest(
                email = email,
                token = token,
                socialProvider = provider,
                socialId = socialId,
                nickname = nickname,
                profileImage = profileImage
            )
            val response = authApiService.socialLogin(request)

            if(response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(RuntimeException("소셜 로그인 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserId(): Result<String> {
        return getMyProfile().map { user -> user.id.toString() }
    }

    suspend fun getMyProfile(): Result<User> {
        return try {
            val response = authApiService.getMyProfile()

            if(response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(RuntimeException("프로필 정보 가져오기 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val response = authApiService.logout()

            if(response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("서버 로그아웃 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotificationSetting(type: String, enabled: Boolean): Result<Unit> {
        return try {
            val request = NotificationRequest(type, enabled)
            val response = authApiService.updateNotificationSetting(request)

            if(response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("변경 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdraw(): Result<Unit> {
        return try {
            val response = authApiService.withdraw()

            if(response.isSuccessful && response.body()?.success == true) {
                // 탈퇴 성공
                tokenManager.deleteToken()
                Result.success(Unit)
            } else {
                Result.failure(kotlin.RuntimeException("회원 탈퇴 실패: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(nickname: String, profileImageUrl: String?): Result<Unit> {
        return try {
            val token = tokenManager.getToken() ?: return Result.failure(Exception("토큰 없음"))
            val response = authApiService.updateProfile(
                token = "Bearer $token",
                request = UpdateProfileRequest(nickname, profileImageUrl)
            )

            if (response.isSuccessful) { // ✅ Response 객체이므로 isSuccessful 확인
                val body = response.body()
                if (body?.success == true) Result.success(Unit)
                else Result.failure(Exception(body?.message ?: "수정 실패"))
            } else {
                Result.failure(Exception("서버 에러: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}