package com.example.travelapp

import android.app.Application
import android.util.Log
import com.example.travelapp.BuildConfig
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 다른 초기화 코드들

        // Kakao SDK 초기화
        Log.d("KakaoSDK_Test", "KAKAO_NATIVE_APP_KEY: ${BuildConfig.KAKAO_NATIVE_APP_KEY}")
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
