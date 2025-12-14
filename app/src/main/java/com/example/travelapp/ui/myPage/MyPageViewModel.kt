package com.example.travelapp.ui.myPage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.travelapp.data.model.User
import com.example.travelapp.data.repository.AuthRepository
import com.example.travelapp.data.repository.CommentRepository
import com.example.travelapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
open class MyPageViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    fun fetchUserInfo() {
        viewModelScope.launch {
            authRepository.getMyProfile()
                .onSuccess { user ->
                    val localPushActivity = tokenManager.getPushActivityOrNull()
                    val localPushMarketing = tokenManager.getPushMarketingOrNull()

                    val mergedUser = user.copy(
                        pushActivity = localPushActivity ?: user.pushActivity,
                        pushMarketing = localPushMarketing ?: user.pushMarketing
                    )

                    Log.d("MY_PAGE_DEBUG", "DB에서 온 값 확인: 활동알림=${user.pushActivity}")
                    _userState.value = mergedUser // 성공하면 user 객체가 바로 들어옴
                }
                .onFailure { e ->
                    e.printStackTrace() // 실패하면 예외(e)가 들어옴
                }
        }
    }

    fun logout(navController: NavController) {
        viewModelScope.launch {
            tokenManager.clearToken()

            withContext(Dispatchers.Main) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    fun updateNotificationSetting(type: String, enabled: Boolean) {
        viewModelScope.launch {
            Log.d("MY_PAGE_DEBUG", "1. 변경 요청 시작: type=$type, value=$enabled")

            val currentUser = _userState.value
            if (currentUser != null) {
                _userState.value = if (type == "activity") {
                    tokenManager.savePushActivity(enabled)
                    currentUser.copy(pushActivity = enabled)
                } else {
                    tokenManager.savePushMarketing(enabled)
                    currentUser.copy(pushMarketing = enabled)
                }
            }

            val result = authRepository.updateNotificationSetting(type, enabled)

            if(result.isSuccess) {
                Log.d("MY_PAGE_DEBUG", "2. 서버 성공!")
            } else {
                val error = result.exceptionOrNull()
                Log.e("MY_PAGE_DEBUG", "3. 서버 실패 ㅠㅠ 원인: ${error?.message}")
            }
        }
    }
}