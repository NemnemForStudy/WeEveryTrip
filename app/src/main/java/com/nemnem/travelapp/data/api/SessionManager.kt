package com.nemnem.travelapp.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    // 세션 상태 관리하는 Flow
    private val _isSessionValid = MutableStateFlow(true)
    val isSessionValid: StateFlow<Boolean> = _isSessionValid.asStateFlow()

    // 세션 만료 알림
    fun logout() {
        _isSessionValid.value = false
    }

    // 로그인 성공 시 상태 초기화
    fun resetSession() {
        _isSessionValid.value = true
    }
}