package com.example.travelapp.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.travelapp.BuildConfig
import com.example.travelapp.util.AuthInterceptor
import com.example.travelapp.data.api.AuthApiService
import com.example.travelapp.data.api.CommentApiService
import com.example.travelapp.data.api.NaverAuthApiService
import com.example.travelapp.data.api.PostApiService
import com.kakao.sdk.auth.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton


@Module // 이 클래스가 Hilt 모듈임을 나타냄.
@InstallIn(SingletonComponent::class) // 이 모듈의 의존성이 앱의 싱글톤 라이프사이클을 따흔다.
object NetworkModule {
    // HTTP 요청 및 응답 로깅을 위한 Interceptor를 제공함.
    // 디버깅 시 네트워크 통신 내용을 콘솔에서 확인할 수 있어 유용함.
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 요청, 응답 본문까지 모두 로깅
        }
    }

    // 인증이 필요없는 클라이언트
    // 네이버 API 호출 시 AuthInterceptor가 필요 없으므로
    @Provides
    @Singleton
    @Named("DefaultOkHttpClient") // Hilt가 구분할 수 있도록 이름 지정
    fun provideDefaultOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // 인증 토큰(JWT)이 포함된 클라이언트 (게시글 작성, 조회 등)
    // AuthInterceptor가 여기서 자동으로 헤더를 붙여줍니다.
    @Provides
    @Singleton
    @Named("AuthOkHttpClient")
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    // 우리 앱 서버용 Retrofit (AuthOkHttpClient 사용)
    // 여기서 AuthOkHttpClient 사용하므로, Retrofit으로 만든 API는 자동으로 토큰이 붙음.
    @Provides
    @Singleton
    @Named("AppRetrofit")
    fun provideAppRetrofit(
        @Named("AuthOkHttpClient") okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): Retrofit {
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

        val baseUrl = if (isEmulator) {
            BuildConfig.BASE_URL
        } else {
            phoneBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.BASE_URL
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 네이버 API 전용 Retrofit 새로 만들자.
    @Provides
    @Singleton
    @Named("NaverRetrofit")
    fun provideNaverRetrofit(@Named("DefaultOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        val NAVER_BASE_URL = "https://openapi.naver.com/"
        return Retrofit.Builder()
            .baseUrl(NAVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // PostApiService 인터페이스 구현체를 Retrofit을 통해 생성해 제공함.
    // 위에 AppRetrofit으로 바꿨으니 변경해주자
    @Provides
    @Singleton
    fun providePostApiService(@Named("AppRetrofit") retrofit: Retrofit): PostApiService {
        return retrofit.create(PostApiService::class.java)
    }

    // 인증 API (로그인/회원가입)
    // 로그인 요청 시에는 토큰이 없으므로 AuthInterceptor가 있어도 헤더에 추가하지 않습니다(Safe).
    // 따라서 그냥 AppRetrofit을 같이 써도 무방합니다.
    @Provides
    @Singleton
    fun provideAuthApiService(@Named("AppRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    // NaverAuthApiService는 NaverRetrofit 사용하도록 새로 추가
    // 네이버 인증 API
    @Provides
    @Singleton
    fun provideNaverAuthApiService(@Named("NaverRetrofit") retrofit: Retrofit): NaverAuthApiService {
        return retrofit.create(NaverAuthApiService::class.java)
    }

    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    @Provides
    @Singleton
    fun provideCommentApiService(@Named("AppRetrofit") retrofit: Retrofit): CommentApiService {
        return retrofit.create(CommentApiService::class.java)
    }
}