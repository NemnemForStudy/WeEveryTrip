package com.example.travelapp.ui.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.api.NaverAuthApiService
import com.example.travelapp.data.model.SocialLoginRequest
import com.example.travelapp.data.model.User
import com.example.travelapp.data.repository.AuthRepository
import com.example.travelapp.util.TokenManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val loginSuccessUser: User? = null,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val naverAuthApiService: NaverAuthApiService
) : ViewModel() {

    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    private val appName = "MoyeoLog"
    private val TAG = "LoginViewModel"


    fun loginWithNaver(context: Context) {
        viewModelScope.launch {
            try {
                NaverIdLoginSDK.initialize(
                    context,
                    BuildConfig.NAVER_CLIENT_ID,
                    BuildConfig.NAVER_CLIENT_SECRET,
                    appName
                )
                NaverIdLoginSDK.authenticate(context, object : OAuthLoginCallback {
                    override fun onSuccess() {
                        fetchNaverUserProfile()
                    }

                    override fun onFailure(httpStatus: Int, message: String) {
                        viewModelScope.launch {
                            val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                            val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                            Log.e(TAG, "네이버 로그인 실패 - 코드: $errorCode, 설명: $errorDescription")
                            emitLoginFailed("네이버 로그인에 실패했습니다.")
                        }
                    }

                    override fun onError(errorCode: Int, message: String) {
                        onFailure(errorCode, message)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "네이버 로그인 SDK 초기화 또는 인증 요청 실패", e)
                emitLoginFailed("네이버 로그인 중 오류가 발생했습니다.")
            }
        }
    }

    private fun fetchNaverUserProfile() {
        viewModelScope.launch {
            val accessToken = NaverIdLoginSDK.getAccessToken()
            if (accessToken == null) {
                emitLoginFailed("네이버 접근 토큰을 가져오는데 실패했습니다.")
                return@launch
            }

            try {
                val response = naverAuthApiService.getNaverUserProfile("Bearer $accessToken")

                // [수정] isSuccessful 오타를 수정하고, 실패 시 에러 메시지를 보내는 로직을 추가합니다.
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!.response
                    Log.i(TAG, "네이버 프로필 조회 성공. 서버에 로그인 요청 시작.")
                    requestSocialLogin(
                        email = profile.email,
                        nickname = profile.nickname,
                        profileImage = profile.profileImage,
                        provider = "naver",
                        socialId = profile.id
                    )
                } else {
                    Log.e(TAG, "네이버 프로필 조회 실패: ${response.code()} ${response.message()}")
                    emitLoginFailed("네이버 사용자 정보를 가져오는데 실패했습니다.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "네이버 프로필 조회 중 오류 발생", e)
                emitLoginFailed("네이버 프로필 조회 중 오류가 발생했습니다.")
            }
        }
    }

    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.i(TAG, "구글 로그인 성공. 서버에 로그인 요청 시작.")

                val userEmail = account.email
                val userId = account.id

                if (userEmail != null && userId != null) {
                    requestSocialLogin(
                        email = userEmail,
                        nickname = account.displayName,
                        profileImage = account.photoUrl?.toString(),
                        provider = "google",
                        socialId = userId
                    )
                } else {
                    Log.e(TAG, "Google 로그인 실패: email 또는 id가 null입니다.")
                    emitLoginFailed("구글 계정 정보를 가져오는데 실패했습니다.")
                }
            } else {
                Log.e(TAG, "구글 로그인 실패: account is null")
                emitLoginFailed("구글 로그인 정보를 가져오는데 실패했습니다.")
            }
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                10 -> "개발자 콘솔에 등록된 앱과 서명(SHA-1)이 일치하지 않습니다."
                12500 -> "Google Play 서비스 업데이트가 필요하거나, 로그인 설정에 문제가 있습니다."
                12501 -> "사용자가 로그인을 취소했습니다."
                else -> "구글 로그인 실패: ${e.statusCode}"
            }
            Log.e(TAG, "Google Sign-In failed with ApiException: ${e.statusCode}", e)
            emitLoginFailed(errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred during Google Sign-In", e)
            emitLoginFailed("예상치 못한 오류가 발생했습니다.")
        }
    }

    private fun requestSocialLogin(email: String, nickname: String?, profileImage: String?, provider: String, socialId: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)
            try {
                val request = SocialLoginRequest(email, nickname, profileImage, provider, socialId)
                val response = authRepository.socialLogin(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!
                    loginData.token?.let { tokenManager.saveToken(it) }
                    _loginUiState.value = LoginUiState(loginSuccessUser = loginData.user)
                    _loginEvent.emit(LoginEvent.LoginSuccess)
                } else {
                    _loginUiState.value = LoginUiState(error = "서버 로그인 실패: ${response.code()}")
                    emitLoginFailed("서버 로그인에 실패했습니다.")
                }
            } catch (e: Exception) {
                _loginUiState.value = LoginUiState(error = "서버 통신 오류: ${e.message}")
                emitLoginFailed("로그인 중 오류가 발생했습니다.")
            }
        }
    }

    fun emitLoginFailed(message: String) {
        viewModelScope.launch {
            _loginEvent.emit(LoginEvent.LoginFailed(message))
        }
    }

    fun loginWithKakaoTalk(context: Context) {
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        emitLoginFailed("카카오톡 로그인이 취소되었습니다.")
                        return@loginWithKakaoTalk
                    }
                    loginWithKakaoAccount(context)
                } else if (token != null) {
                    handleKakaoLoginSuccess(token.accessToken)
                }
            }
        } else {
            loginWithKakaoAccount(context)
        }
    }

    private fun loginWithKakaoAccount(context: Context) {
        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            if (error != null) {
                emitLoginFailed("카카오계정 로그인 실패: ${error.message}")
            } else if (token != null) {
                handleKakaoLoginSuccess(token.accessToken)
            }
        }
    }

    private fun handleKakaoLoginSuccess(accessToken: String) {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "카카오 사용자 정보 요청 실패", error)
                emitLoginFailed("카카오 사용자 정보를 가져오는데 실패했습니다.")
            } else if (user != null) {
                Log.i(TAG, "카카오 사용자 정보 요청 성공. 서버에 로그인 요청 시작.")

                val userEmail = user.kakaoAccount?.email
                val userId = user.id

                if (userEmail != null && userId != null) {
                    requestSocialLogin(
                        email = userEmail,
                        nickname = user.kakaoAccount?.profile?.nickname,
                        profileImage = user.kakaoAccount?.profile?.thumbnailImageUrl,
                        provider = "kakao",
                        socialId = userId.toString()
                    )
                } else {
                    Log.e(TAG, "Kakao 로그인 실패: email 또는 id가 null입니다.")
                    emitLoginFailed("카카오 계정 정보를 가져오는데 실패했습니다.")
                }
            }
        }
    }
}

sealed class LoginEvent {
    object LoginSuccess : LoginEvent()
    data class LoginFailed(val message: String) : LoginEvent()
}