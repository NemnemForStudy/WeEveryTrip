package com.example.travelapp.ui.myPage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class MyPageViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    // 사용자 정보 (일단 하드코딩, 나중에 API 연동)
    private val _nickname = MutableStateFlow("넴넴")
    val nickname = _nickname.asStateFlow()

    private val _email = MutableStateFlow("Nemnem@naver.com")
    val email = _email.asStateFlow()

    private val _postCount = MutableStateFlow(12)
    val postCount = _postCount.asStateFlow()

    private val _likeCount = MutableStateFlow(48)
    val likeCount = _likeCount.asStateFlow()

    private val _commentCount = MutableStateFlow(23)
    val commentCount = _commentCount.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
        }
    }
}