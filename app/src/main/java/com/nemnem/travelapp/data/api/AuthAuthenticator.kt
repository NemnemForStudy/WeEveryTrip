package com.nemnem.travelapp.data.api

import android.util.Log
import com.nemnem.travelapp.util.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

/**
 * 401 Unauthorized 에러 발생 시 토큰 갱신을 담당하는 클래스
 */
class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager,
    // 순환 참조 방지를 위해 Provider를 사용합니다 (Hilt에서 필요)
    @Named("RefreshApiService") private val authApiProvider: Provider<AuthApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        if(response.request.url.encodedPath.contains("/api/auth/refresh")) {
            sessionManager.logout()
            return null
        }
        // 1. 무한 루프 방지 (재시도 횟수가 3번 이상이면 포기)
        if (response.count() >= 3) {
            Log.e("ModuTrip_Auth", "재시도 횟수 초과로 로그아웃 처리합니다.")
            sessionManager.logout()
            return null
        }

        synchronized(this) {
            Log.d("ModuTrip_Auth", "🚨 401 에러 감지: 토큰 갱신을 시작합니다.") // 로그 추가

            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")

            if(currentToken != requestToken && !currentToken.isNullOrEmpty()) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken()
            if(refreshToken.isNullOrEmpty()) {
                sessionManager.logout()
                return null
            }

            return try {
                val refreshResponse = authApiProvider.get().refreshTokens("Bearer $refreshToken").execute()

                if(refreshResponse.isSuccessful) {
                    Log.d("ModuTrip_Auth", "✨ 토큰 갱신 성공! 새로운 토큰으로 요청을 재시도합니다.") // 로그 추가

                    val newTokens = refreshResponse.body()
                    if(newTokens != null) {
                        // 새 토큰 안전하게 저장
                        tokenManager.saveAccessToken(newTokens.accessToken)
                        tokenManager.saveRefreshToken(newTokens.refreshToken)
                        tokenManager.saveToken(newTokens.accessToken)

                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    } else null
                } else {
                    Log.e("ModuTrip_Auth", "❌ 토큰 갱신 실패 (리프레시 토큰 만료 등)") // 로그 추가
                    sessionManager.logout()
                    null
                }
            } catch (e: Exception) {
                Log.e("ModuTrip_Auth", "네트워크 오류: ${e.message}")
                null
            }
        }
    }

    // 재시도 횟수를 체크하기 위한 확장 함수
    private fun Response.count(): Int {
        var result = 1
        var response = priorResponse
        while (response != null) {
            result++
            response = response.priorResponse
        }
        return result
    }
}