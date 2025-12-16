package com.example.travelapp.data.repository

import androidx.compose.ui.geometry.Rect
import com.example.travelapp.data.api.AuthApiService
import com.example.travelapp.data.model.Setting.NotificationRequest
import com.example.travelapp.data.model.SocialLoginRequest
import com.example.travelapp.data.model.SocialLoginResponse
import com.example.travelapp.data.model.User
import com.example.travelapp.util.TokenManager
import retrofit2.Response
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
    suspend fun socialLogin(provider: String, token: String, email: String, socialId: String): Result<Boolean> {
        return try {
            val request = SocialLoginRequest(
                email = email,
                socialProvider = provider,
                socialId = socialId
            )
            val response = authApiService.socialLogin(request)

            if(response.isSuccessful && response.body() != null) {
                val tokenString = response.body()!!.token
                tokenManager.saveToken(tokenString)
                Result.success(true)
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

    // 일반 로그인
    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            val request = SocialLoginRequest(
                email = email,
                socialProvider = "EMAIL",  // 또는 적절한 provider 값
                socialId = email  // 이메일 로그인의 경우 email을 socialId로 사용
            )
            val response = authApiService.socialLogin(request)

            if(response.isSuccessful && response.body() != null) {
                val tokenString = response.body()!!.token
                tokenManager.saveToken(tokenString)
                Result.success(true)
            } else {
                Result.failure(RuntimeException("일반 로그인 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}