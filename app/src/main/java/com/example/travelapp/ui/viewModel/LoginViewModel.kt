package com.example.travelapp.ui.viewModel

import android.content.ContentValues.TAG
import android.content.Context
import android.nfc.Tag

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


// Hilt가 생성자를 사용해 ViewModel을 만들도록 알림.
class LoginViewModel : ViewModel() {
    // 로그인 성공 / 실패 이벤트를 단발성 UI에  전달하기 위한 SharedFlow
    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()
    val appName = "MoyeoLog"
    val TAG = "LoginViewModel"

    /**
     * 네이버 로그인 API 호출 결과 처리하는 공통 콜백 함수.
     * authenticate() 메소드 마라미터로 전달되어 로그인 결과를 비동기적으로 받음.
     */

    private val oauthLoginCallback = object : OAuthLoginCallback {
        override fun onSuccess() {
            viewModelScope.launch {
                // 로그인 성공 시, 서버로 보낼 접근 토큰은 NaverIdLoginSDK.getAccessToken()으로 얻을 수 있음.
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
    }

    /**
     * 네이버 로그인을 시도하는 메인 함수
     * 네이버 SDK 초기화, 인증 요청
     * @param context Activity Context 필요.
     */

    fun loginWithNaver(context: Context) {
        viewModelScope.launch {
            try {
                // 1. 네이버 SDK 초기화
                // 네이버 개발자 센터에서 발급받은 클라이언트 ID, 시크릿, 이름 입력
                NaverIdLoginSDK.initialize(
                    context,
                    BuildConfig.NAVER_CLIENT_ID,
                    BuildConfig.NAVER_CLIENT_SECRET,
                    appName
                )

                // 2. 네이버 로그인 인증 요청
                // oauthLoginCallback을 통해 비동기적으로 결과를 받음.
                NaverIdLoginSDK.authenticate(context, oauthLoginCallback)
            } catch (e: Exception) {
                Log.e(TAG, "네이버 로그인 SDK 초기화 또는 인증 요청 실패", e)
                _loginEvent.emit(LoginEvent.LoginFailed("네이버 로그인 중 오류가 발생했습니다."))
            }
        }
    }

    fun handlerGoogleSignInResult(account: GoogleSignInAccount) {
        viewModelScope.launch {
            // idToken은 사용자 인증하기 위해 백엔드 서버로 보내야 하는 중요 정보다.
            // 이 토큰을 서버로 보내 유효성 검증하고 앱 자체 토큰을 발급받는 과정 필요
            val idToken = account.idToken
            if (idToken != null) {
                Log.i(TAG, "구글 로그인 성공, Id 토큰: ${idToken}")
                // TODO: 여기서 idToken 백엔드로 보내야함.

                // 로그인 성공 이벤트만 전달
                _loginEvent.emit(LoginEvent.LoginSuccess)
            } else {
                // idToken 없는 경우(예외 처리)
                Log.e(TAG, "구글 로그인 실패: ID 토큰을 얻을 수 없습니다")
                _loginEvent.emit(LoginEvent.LoginFailed("구글 로그인 정보를 가져오는데 실패했습니다."))
            }
        }
    }

    // 카카오 로그인 API 호출 결과를 처리하는 공통 콜백 함수
    private val kakaoLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        viewModelScope.launch {
            if (error != null) {
                Log.e(TAG, "카카오계정으로 로그인 실패", error)
                _loginEvent.emit(LoginEvent.LoginFailed("카카오 로그인에 실패했습니다."))
            } else if (token != null) {
                Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
                // 여기서 서버로 토큰을 보내거나, 사용자 정보를 가져오는 등 추가 로직 수행 가능
                _loginEvent.emit(LoginEvent.LoginSuccess)
            }
        }
    }
    /**
     * 카카오 로그인을 시도하는 메인 함수.
     * 카카오톡 앱이 있으면 앱으로, 없으면 카카오 계정 웹페이지로 로그인을 시도합니다.
     * @param context Activity Context가 필요합니다.
     */

    fun loginWithKakaoTalk(context: Context) {
        // 가톡 설치 되어있으면 카톡 로그인, 아니면 카카오 계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Log.e(TAG, "카카오톡으로 로그인 실패", error)

                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소할 경우
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리(ex. 뒤로가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        viewModelScope.launch {
                            _loginEvent.emit(LoginEvent.LoginFailed("카카오톡 로그인 취소"))
                        }
                        return@loginWithKakaoTalk
                    }
                    // 카톡에 연결된 계정이 없는 경우. 카카오 계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(
                        context,
                        callback = kakaoLoginCallback
                    )
                } else if (token != null) {
                    Log.i(TAG, "카카오톡으로 로그인 성공 ${token.accessToken}")
                    viewModelScope.launch {
                        _loginEvent.emit(LoginEvent.LoginSuccess)
                    }
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = kakaoLoginCallback)
        }
    }
}

// ViewModel이 UI로 전달할 이벤트 정의
sealed class LoginEvent {
    object LoginSuccess : LoginEvent()
    data class LoginFailed(val message: String) : LoginEvent()
}