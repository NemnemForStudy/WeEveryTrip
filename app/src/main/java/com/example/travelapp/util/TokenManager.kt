package com.example.travelapp.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JWT 토큰을 SharedPreferences에 저장하고 관리하는 클래스.
 * @param context 애플리케이션 컨텍스트, Hilt에 의해 주입됨.
 */

@Singleton // 앱 전역에서 하나의 인스턴스만 사용하도록 싱글톤 지정.
class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    // auth_prefs 라는 이름의 SharedPreferences 인스턴스를 생성
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // 토큰 저장할때 사용할 키 정의
    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }

    /**
     * 서버로부터 받은 JWT 토큰을 SharedPreferences에 저장
     * @param token 저장할 토큰 문자열
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    /**
     * 저장된 JWT 토큰을 불러옴
     * @return 저장된 토큰 문자열, 토큰이 없으면 null 반환.
     */
    fun getToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * 저장된 토큰을 삭제함. (로그아웃 시 사용)
     */

    fun clearToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    fun isTokenValid(): Boolean {
        val token = getToken()
        return !token.isNullOrEmpty()
    }
}