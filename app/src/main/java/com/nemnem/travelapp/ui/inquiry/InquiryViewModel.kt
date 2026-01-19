package com.nemnem.travelapp.ui.inquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nemnem.travelapp.data.api.InquiryApiService
import com.nemnem.travelapp.data.model.email.InquiryRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 문의하기 화면의 상태를 정의하는 Sealed Interface
 * UI는 이 상태에 따라 로딩 바를 보여줄지, 뒤로 가기를 할지 결정합니다.
 */
// 딱 하나의 상태만 존재함을 보장함.
// 무조건 단 하나의 상태에만 머물게 된다.
// Compose화면에서 when 문 사용해 UI 그릴 때, Sealed Interface 사용하면 컴파일러가 모든 경우의 수를 앎.
// 상태마다 필요한 정보를 다르게 담을 수 있다.
sealed interface InquiryUiState {
    object Idle : InquiryUiState
    object Loading : InquiryUiState // 서버 전송 중 상태
    object Success : InquiryUiState // 성공 상태
    data class Error(val message: String) : InquiryUiState
}

@HiltViewModel
class InquiryViewModel @Inject constructor(
    private val inquiryApiService: InquiryApiService
) : ViewModel() {
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    private val _email = MutableStateFlow("")

    private val _uiState = MutableStateFlow<InquiryUiState>(InquiryUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // 입력값 변경 함수
    fun onTitleChanged(newTitle: String) { _title.value = newTitle }
    fun onContentChanged(newContent: String) { _content.value = newContent }
    fun onEmailChanged(newEmail: String) { _email.value = newEmail }

    // 화면 열릴 때 초기 이메일 설정
    fun setUserEmail(email: String) {
        _email.value = email
    }

    fun sendEmail() {
        if(_title.value.isBlank() || _content.value.isBlank()) {
            _uiState.value = InquiryUiState.Error("모든 항목을 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = InquiryUiState.Loading // 로딩 시작

            try {
                // 요청 객체 생성
                val request = InquiryRequest(
                    title = _title.value,
                    content = _content.value,
                    email = _email.value
                )

                // 서버 호출 (Retrofit)
                val response = inquiryApiService.sendEmail(request)

                if(response.isSuccessful) {
                    _uiState.value = InquiryUiState.Success
                } else {
                    _uiState.value = InquiryUiState.Error("서버 응답 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace() // 로그캣에 상세 에러 출력
                _uiState.value = InquiryUiState.Error("에러 발생: ${e.message}")
            }
        }
    }

    // 상태 초기화
    fun resetState() {
        _uiState.value = InquiryUiState.Idle
    }
}