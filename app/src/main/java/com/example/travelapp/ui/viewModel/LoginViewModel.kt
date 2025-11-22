package com.example.travelapp.ui.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

// Hilt가 이 ViewModel을 생성할 수 있도록 @HiltViewModel 어노테이션을 사용하는 것이 좋습니다.
// 다만, 현재 LoginScreen에서 hiltViewModel()로 주입받고 있으므로 클래스 선언은 그대로 둡니다.
class LoginViewModel : ViewModel() {

    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent: SharedFlow<LoginEvent> = _loginEvent

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
                        viewModelScope.launch {
                            Log.i(TAG, "네이버 로그인 성공, 토큰 : ${NaverIdLoginSDK.getAccessToken()}")
                            _loginEvent.emit(LoginEvent.LoginSuccess)
                        }
                    }

                    override fun onFailure(httpStatus: Int, message: String) {
                        viewModelScope.launch {
                            val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                            val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                            Log.e(TAG, "네이버 로그인 실패 - 코드: $errorCode, 설명: $errorDescription")
                            _loginEvent.emit(LoginEvent.LoginFailed("네이버 로그인에 실패했습니다."))
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


    fun handlerGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                Log.i(TAG, "구글 로그인 성공, ID 토큰 획득: $idToken")
                firebaseAuthWithGoogle(idToken)
            } else {
                Log.e(TAG, "구글 로그인 실패: ID 토큰을 얻을 수 없습니다.")
                emitLoginFailed("구글 로그인 정보를 가져오는데 실패했습니다. (토큰 없음)")
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

    private fun firebaseAuthWithGoogle(idToken: String) {
        // TODO: 여기서 idToken을 백엔드 서버로 보내 유효성을 검증하고, 앱 자체 JWT를 발급받아야 합니다.
        viewModelScope.launch {
            Log.i(TAG, "서버로 ID 토큰 전송: $idToken")
            _loginEvent.emit(LoginEvent.LoginSuccess)
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
        viewModelScope.launch {
            Log.i(TAG, "카카오 로그인 성공: $accessToken")
            _loginEvent.emit(LoginEvent.LoginSuccess)
        }
    }
}

sealed class LoginEvent {
    object LoginSuccess : LoginEvent()
    data class LoginFailed(val message: String) : LoginEvent()
}