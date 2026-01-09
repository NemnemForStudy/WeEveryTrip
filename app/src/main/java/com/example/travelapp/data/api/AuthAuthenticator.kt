package com.example.travelapp.data.api

import android.util.Log
import com.example.travelapp.data.model.TokenResponse
import com.example.travelapp.util.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * 401 Unauthorized 에러 발생 시 토큰 갱신을 담당하는 클래스
 */
class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager,
    // 순환 참조 방지를 위해 Provider를 사용합니다 (Hilt에서 필요)
    private val authApiProvider: Provider<AuthApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("ModuTrip_Auth", "★★★ Authenticator 진입: 401 에러 감지 ★★★")

        // 1. 무한 루프 방지 (재시도 횟수가 3번 이상이면 포기)
        if (response.count() >= 3) {
            Log.e("ModuTrip_Auth", "재시도 횟수 초과로 로그아웃 처리합니다.")
            sessionManager.logout()
            return null
        }

        // 2. 저장된 Refresh Token 가져오기
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            Log.e("ModuTrip_Auth", "Refresh Token이 없어 로그아웃 처리합니다.")
            sessionManager.logout()
            return null
        }

        // 3. 서버에 새 토큰 요청 (동기 방식 .execute() 사용 필수)
        // 주의: Authenticator는 백그라운드에서 돌기 때문에 코루틴 suspend를 쓰지 않고 Call을 씁니다.
        val refreshCall = authApiProvider.get().refreshTokens("Bearer $refreshToken")

        return try {
            val refreshResponse = refreshCall.execute()

            if (refreshResponse.isSuccessful) {
                val newTokens = refreshResponse.body()
                if (newTokens != null) {
                    Log.d("ModuTrip_Auth", "토큰 갱신 성공! 새 토큰을 저장하고 재시도합니다.")

                    // 4. 새 토큰들을 EncryptedSharedPreferences에 저장
                    tokenManager.saveAccessToken(newTokens.accessToken)
                    tokenManager.saveRefreshToken(newTokens.refreshToken)
                    tokenManager.saveToken(newTokens.accessToken) // 기존 키 호환용

                    // 5. 실패했던 원래 요청에 새 토큰을 끼워넣어 다시 보냄
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } else {
                    null
                }
            } else {
                // Refresh Token도 만료된 경우 (401 or 403 등)
                Log.e("ModuTrip_Auth", "토큰 갱신 실패 (서버 에러): ${refreshResponse.code()}")
                sessionManager.logout()
                null
            }
        } catch (e: Exception) {
            Log.e("ModuTrip_Auth", "네트워크 오류로 토큰 갱신 실패: ${e.message}")
            sessionManager.logout()
            null
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