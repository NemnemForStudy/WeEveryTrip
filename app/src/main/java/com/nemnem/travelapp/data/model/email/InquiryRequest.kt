package com.nemnem.travelapp.data.model.email

import com.google.gson.annotations.SerializedName

/**
 * 문의하기 요청 데이터 모델
 * @SerializedName: 서버에서 받는 JSON 키 값과 코틀린 변수명을 연결해줍니다.
 * 나중에 앱을 배포할 때 코드가 난독화되어 변수명이 바뀌어도 서버 통신이 깨지지 않게 해주는 필수 장치입니다.
 */
data class InquiryRequest (
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("email")
    val email: String
)