package com.example.travelapp.ui.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.BuildConfig
import com.example.travelapp.data.repository.AuthRepository
import com.example.travelapp.util.TokenManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.annotation.meta.TypeQualifierNickname
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class LoginEvent {
    object LoginSuccess : LoginEvent()
    data class LoginFailed(val message: String) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
    // NaverAuthApiService는 이제 필요 없습니다. (앱에서 프로필 조회 안 함)
) : ViewModel() {

    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    private val appName = "MoyeoLog"
    private val TAG = "LoginViewModel"

    // ------------------------------------------------------------------------
    // 1. 네이버 로그인
    // ------------------------------------------------------------------------
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
                        val accessToken = NaverIdLoginSDK.getAccessToken() ?: run {
                            emitLoginFailed("네이버 토큰을 가져오지 못했습니다.")
                            return
                        }

                        // 프로필 정보 요청
                        NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                            override fun onSuccess(response: NidProfileResponse) {
                                val email = response.profile?.email
                                val id = response.profile?.id?.toString()
                                val nickname = response.profile?.nickname
                                val profileImage = response.profile?.profileImage

                                if (email != null && id != null) {
                                    requestSocialLogin(
                                        provider = "NAVER",
                                        token = accessToken,
                                        email = email,
                                        socialId = id,
                                        nickname = nickname,
                                        profileImage = profileImage
                                    )
                                } else {
                                    emitLoginFailed("네이버 프로필 정보를 가져오지 못했습니다.")
                                }
                            }

                            override fun onError(errorCode: Int, message: String) {
                                emitLoginFailed("네이버 프로필 조회 실패: $message")
                            }

                            override fun onFailure(httpStatus: Int, message: String) {
                                emitLoginFailed("네이버 프로필 조회 실패: $message")
                            }
                        })
                    }

                    override fun onFailure(httpStatus: Int, message: String) {
                        val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                        val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                        Log.e(TAG, "네이버 로그인 실패: $errorCode, $errorDescription")
                        emitLoginFailed("네이버 로그인 실패: $message")
                    }

                    override fun onError(errorCode: Int, message: String) {
                        onFailure(errorCode, message)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "네이버 로그인 예외", e)
                emitLoginFailed("네이버 로그인 중 오류가 발생했습니다.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. 카카오 로그인
    // ------------------------------------------------------------------------
    fun loginWithKakaoTalk(context: Context) {
        // 카카오톡 설치 여부 확인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk // 사용자가 취소
                    }
                    // 카카오톡 로그인 실패 시 카카오계정 로그인 시도
                    loginWithKakaoAccount(context)
                } else if (token != null) {
                    // 성공 시 사용자 정보 조회 후 서버로 전송
                    UserApiClient.instance.me { user, error ->
                        if (error != null) {
                            emitLoginFailed("카카오 사용자 정보를 가져오지 못했습니다.")
                        } else if (user != null) {
                            val email = user.kakaoAccount?.email
                            val id = user.id.toString()
                            val nickname = user.kakaoAccount?.profile?.nickname
                            val profileImage = user.kakaoAccount?.profile?.thumbnailImageUrl

                            if (email != null) {
                                requestSocialLogin(
                                    provider = "KAKAO",
                                    token = token.accessToken,
                                    email = email,
                                    socialId = id,
                                    nickname = nickname,
                                    profileImage = profileImage
                                )
                            } else {
                                emitLoginFailed("이메일 동의가 필요합니다.")
                            }
                        }
                    }
                }
            }
        } else {
            loginWithKakaoAccount(context)
        }
    }

    private fun loginWithKakaoAccount(context: Context) {
        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            if (error != null) {
                Log.e(TAG, "카카오 계정 로그인 실패", error)
                emitLoginFailed("카카오 로그인에 실패했습니다.")
            } else if (token != null) {
                UserApiClient.instance.me { user, error ->
                    if (error != null) {
                        emitLoginFailed("카카오 사용자 정보를 가져오지 못했습니다.")
                    } else if (user != null) {
                        val email = user.kakaoAccount?.email
                        val id = user.id.toString()
                        val nickname = user.kakaoAccount?.profile?.nickname
                        val profileImage = user.kakaoAccount?.profile?.thumbnailImageUrl

                        if (email != null) {
                            requestSocialLogin(
                                provider = "KAKAO",
                                token = token.accessToken,
                                email = email,
                                socialId = id,
                                nickname = nickname,
                                profileImage = profileImage
                            )
                        } else {
                            emitLoginFailed("이메일 동의가 필요합니다.")
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // 3. 구글 로그인
    // ------------------------------------------------------------------------
    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                // 구글은 idToken을 서버 검증용으로 사용하고, 이메일과 ID를 추출합니다.
                val idToken = account.idToken
                val email = account.email
                val id = account.id

                if (idToken != null && email != null && id != null) {
                    requestSocialLogin(
                        provider = "GOOGLE",
                        token = idToken,
                        email = email,
                        socialId = id,
                        nickname = account.displayName,
                        profileImage = account.photoUrl?.toString()
                    )
                } else {
                    emitLoginFailed("구글 계정 정보를 가져오지 못했습니다.")
                }
            } else {
                emitLoginFailed("구글 계정 정보를 가져오지 못했습니다.")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "구글 로그인 API 예외", e)
            emitLoginFailed("구글 로그인 실패 (코드: ${e.statusCode})")
        }
    }

    // ------------------------------------------------------------------------
    // [공통] 서버로 소셜 로그인 요청
    // ------------------------------------------------------------------------
    private fun requestSocialLogin(
        provider: String,
        token: String,
        email: String,
        socialId: String,
        nickname: String?,
        profileImage: String?
    ) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)

            try {
                val result = authRepository.socialLogin(
                    provider = provider,
                    token = token,
                    email = email,
                    socialId = socialId,
                    nickname = nickname,
                    profileImage = profileImage
                )

                result.onSuccess {
                    _loginUiState.value = LoginUiState(isLoading = false)
                    _loginEvent.emit(LoginEvent.LoginSuccess)
                }.onFailure { e ->
                    _loginUiState.value = LoginUiState(isLoading = false, error = e.message)
                    emitLoginFailed("로그인 실패: ${e.message}")
                }
            } catch (e: Exception) {
                _loginUiState.value = LoginUiState(isLoading = false, error = e.message)
                emitLoginFailed("로그인 처리 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    fun emitLoginFailed(message: String) {
        viewModelScope.launch {
            _loginEvent.emit(LoginEvent.LoginFailed(message))
        }
    }
}