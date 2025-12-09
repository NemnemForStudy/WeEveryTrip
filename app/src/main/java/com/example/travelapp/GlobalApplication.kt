package com.example.travelapp

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // 다른 초기화 코드들

        // Kakao SDK 초기화
        Log.d("KakaoSDK_Test", "KAKAO_NATIVE_APP_KEY: ${BuildConfig.KAKAO_NATIVE_APP_KEY}")
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
    // Coil 전역 ImageLoader 설정
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 디스크 2% 사용
                    .build()
            }
            .crossfade(true)
            .build()
    }
}