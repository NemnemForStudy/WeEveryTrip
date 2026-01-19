package com.nemnem.travelapp.util

import android.os.Build
import com.nemnem.travelapp.BuildConfig
import com.nemnem.travelapp.data.api.PostApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val client = OkHttpClient.Builder().build()

    // 이전에 사용한 URL 결정 로직 활용
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(resolveBaseUrlForDevice())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val postApiService: PostApiService by lazy {
        retrofit.create(PostApiService::class.java)
    }

    private fun resolveBaseUrlForDevice(): String {
        val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))

        val phoneBaseUrl = runCatching {
            BuildConfig::class.java.getField("PHONE_BASE_URL").get(null) as String
        }.getOrNull()

        val raw = if(isEmulator) {
            BuildConfig.BASE_URL
        } else {
            phoneBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL
        }

        return raw.trimEnd('/') + "/"
    }

    private fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                // 저장된 토큰이 있다면 헤더 추가
                tokenManager.getToken()?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }
            .build()
    }
}