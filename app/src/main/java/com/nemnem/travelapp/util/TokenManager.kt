package com.nemnem.travelapp.util

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JWT 토큰을 SharedPreferences에 저장하고 관리하는 클래스.
 * @param context 애플리케이션 컨텍스트, Hilt에 의해 주입됨.
 */

@Singleton // 앱 전역에서 하나의 인스턴스만 사용하도록 싱글톤 지정.
class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    // 보안을 위해 EncryptedSharedPreferences로 변경.
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = try {
        createSharedPrefs(context)
    } catch (e: Exception) {
        // 🛑 에러 발생 시(키 충돌 등) 기존 파일을 삭제하고 새 파일로 초기화
        Log.e("TokenManager", "암호화 복호화 실패! 기존 프리퍼런스 초기화 실행: ${e.message}")

        // 파일 삭제 시도 (파일명이 "secure_auth_prefs"인 경우)
        context.deleteSharedPreferences("secure_auth_prefs")

        // 다시 생성
        createSharedPrefs(context)
    }

    private fun createSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        "secure_auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 토큰 저장할때 사용할 키 정의
    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_PUSH_ACTIVITY = "push_activity"
        private const val KEY_PUSH_MARKETING = "push_marketing"
    }

    /**
     * 서버로부터 받은 JWT 토큰을 SharedPreferences에 저장
     * @param token 저장할 토큰 문자열
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun clearAllTokens() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
    }
    /**
     * 저장된 JWT 토큰을 불러옴
     * @return 저장된 토큰 문자열, 토큰이 없으면 null 반환.
     */
    fun getToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun deleteToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    /**
     * 저장된 토큰을 삭제함. (로그아웃 시 사용)
     */
    fun clearToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    fun savePushActivity(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PUSH_ACTIVITY, enabled).apply()
    }

    fun getPushActivityOrNull(): Boolean? {
        if (!prefs.contains(KEY_PUSH_ACTIVITY)) return null
        return prefs.getBoolean(KEY_PUSH_ACTIVITY, true)
    }

    fun savePushMarketing(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PUSH_MARKETING, enabled).apply()
    }

    fun getPushMarketingOrNull(): Boolean? {
        if (!prefs.contains(KEY_PUSH_MARKETING)) return null
        return prefs.getBoolean(KEY_PUSH_MARKETING, false)
    }

    fun isTokenValid(): Boolean {
        val token = getToken()
        return !token.isNullOrEmpty()
    }
}