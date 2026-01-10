package com.example.travelapp.ui.myPage

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.travelapp.data.model.User
import com.example.travelapp.data.repository.AuthRepository
import com.example.travelapp.data.repository.CommentRepository
import com.example.travelapp.util.RetrofitClient.postApiService
import com.example.travelapp.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
open class MyPageViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    private val _localImageUri = MutableStateFlow<Uri?>(null)
    val localImageUri = _localImageUri.asStateFlow()

    private val _imageVersion = MutableStateFlow(System.currentTimeMillis())
    val imageVersion = _imageVersion.asStateFlow()

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

    fun withdraw(navController: NavController) {
        viewModelScope.launch {
//            val result = authRepository.withdraw()
//
//            result.onSuccess {
//                tokenManager.deleteToken()
//
//                // 메인 스레드에서 로그인 화면으로 이동
//                withContext(Dispatchers.Main) { // 쓰레드 전환. UI를 다시 그리는 메인 쓰레드로 제어권 넘기는 작업.
//                    navController.navigate("login") {
//                        popUpTo(0) { inclusive = true } // 백스택 청소. 0번 지점까지 싹 다 지우라는 의미.
//                    }
//                }
//            }.onFailure { e ->
//                Log.d("MY_PAGE_DEBUG_", "회원 탈퇴 실패: ${e.message}")
//            }

            Log.d("MY_PAGE_DEBUG", "회원 탈퇴 테스트 시작 (시뮬레이션)")
            kotlinx.coroutines.delay(1000) // 1초 기다리기
            // 3. 바로 성공했다고 가정하고 로직 실행
            tokenManager.deleteToken() // 로컬 토큰 삭제

            // 4. 메인 스레드에서 로그인 화면으로 이동
            withContext(Dispatchers.Main) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }

            Log.d("MY_PAGE_DEBUG", "테스트 탈퇴 완료: 로그인 화면으로 이동합니다.")
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

    fun updateProfile(context: Context, newNickname: String?, imageUri: Uri?) {
        viewModelScope.launch {
            if(imageUri != null) {
                // 서버에 올라가기 전에 로컬 URI를 먼저 저장 (즉시 반영됨)
                _localImageUri.value = imageUri
            }
            try {
                // 현재 유저 정보 가져오기
                val currentUser = _userState.value ?: return@launch
                var finalImageUrl = currentUser.profileImageUrl
                val finalNickname = newNickname ?: currentUser.nickname

                // 이미지가 선택되었다면 서버에 업로드
                if(imageUri != null) {
                    val imagePart = uriToPart(context, imageUri)
                    val uploadResponse = postApiService.uploadImages(
                        token = "Bearer ${tokenManager.getToken()}",
                        images = listOf(imagePart)
                    )
                    if (uploadResponse.isSuccessful) {
                        // 업로드 성공 시 서버가 준 새 URL로 교체
                        finalImageUrl = uploadResponse.body()?.urls?.firstOrNull() ?: finalImageUrl
                    } else {
                        Log.e("MyPage", "이미지 업로드 실패: ${uploadResponse.errorBody()?.string()}")
                        return@launch // 업로드 실패 시 중단
                    }
                }

                // 닉네임과 이미지 URL을 최종적으로 업데이트
                val result = authRepository.updateProfile(
                    nickname = finalNickname,
                    profileImageUrl = finalImageUrl
                )

                if(result.isSuccess) {
                    _imageVersion.value = System.currentTimeMillis() // 이때만 새로운 번호를 부여
                    fetchUserInfo()
                    Log.d("MyPage", "프로필 업데이트 성공")
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "수정 실패"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MyPage", "예외 발생: ${e.stackTraceToString()}")
            }
        }
    }

    private fun uriToPart(context: Context, uri: Uri): okhttp3.MultipartBody.Part {
        val contentResolver = context.contentResolver

        // ✅ 권한 문제를 방지하기 위해 즉시 InputStream을 열고 닫습니다.
        return try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IOException("파일을 열 수 없습니다.")

            val bytes = inputStream.use { it.readBytes() }
            val mediaType = (contentResolver.getType(uri) ?: "image/jpeg").toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(mediaType)

            okhttp3.MultipartBody.Part.createFormData(
                "images",
                "profile_${System.currentTimeMillis()}.jpg",
                requestBody
            )
        } catch (e: Exception) {
            Log.e("MyPage", "파일 읽기 에러: ${e.message}")
            throw e
        }
    }
    // MediaType 처리를 위한 확장 함수 (상단 import 확인)
    private fun String.to_media_type_or_null() = toMediaTypeOrNull()

}