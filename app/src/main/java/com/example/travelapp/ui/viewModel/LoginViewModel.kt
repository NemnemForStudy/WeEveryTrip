package com.example.travelapp.ui.viewModel

import android.content.ContentValues.TAG
import android.content.Context
import android.nfc.Tag

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class LoginViewModel : ViewModel() {
    // 로그인 성공 / 실패 이벤트를 단발성 UI에  전달하기 위한 SharedFlow
    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    val TAG = "LoginViewModel"

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