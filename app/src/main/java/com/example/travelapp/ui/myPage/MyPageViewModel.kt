package com.example.travelapp.ui.myPage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.data.api.AuthApiService
import com.example.travelapp.data.model.User
import com.example.travelapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class MyPageViewModel @Inject constructor(
    private val authApiService: AuthApiService,
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

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null

            try {
                val response = authApiService.getMyProfile()
                if(response.isSuccessful) {
                    _user.value = response.body()
                } else {
                    _errorMsg.value = "프로필을 불러올 수 없습니다."
                }
            } catch (e: Exception) {
                _errorMsg.value = "네트워크 오류 ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
        }
    }
}