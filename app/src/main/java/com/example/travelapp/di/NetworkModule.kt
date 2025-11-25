package com.example.travelapp.di

import com.example.travelapp.BuildConfig
import com.example.travelapp.util.AuthInterceptor
import com.example.travelapp.data.api.AuthApiService
import com.example.travelapp.data.api.NaverAuthApiService
import com.example.travelapp.data.api.PostApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttp
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

    // 네이버 API 호출 시 AuthInterceptor가 필요 없으므로
    // OkHttpClient 두 종류로 만들자.
    // 기본 OkHttpClient(로깅 인터셉트 포함)
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

    // 인증 토큰이 필요한 OkHttpClient
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

    // provideOkHttpClient 함수가 AuthInterceptor를 파라미터로 받도록 변경
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor // Hilt가 AuthInterceptor 인스턴스 주입
    ) : OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
        // 모든 요청에 AuthInterceptor 적용되도록 추가
            .addInterceptor(authInterceptor)
            .build()
    }

    // Retrofit 인스턴스 제공
    // OkHttpClient와 BaseURL, Converter Factory 설정
    // AuthOkHttpClient 사용하도록 수정 11-24 20:27
    @Provides
    @Singleton
    @Named("AppRetrofit")
    fun provideAppRetrofit(@Named("AuthOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.PHONE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // JSON 파싱 위한 GSON 컨버팅
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

    // AuthApiService 구현체를 Retrofit을 통해 생성해 제공함.
    // 이제 Hilt는 AuthRepo가 필요하는 AuthApiService를 어떻게 만들지 알게 됨
    @Provides
    @Singleton
    fun provideAuthApiService(@Named("AppRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    // NaverAuthApiService는 NaverRetrofit 사용하도록 새로 추가
    @Provides
    @Singleton
    fun provideNaverAuthApiService(@Named("NaverRetrofit") retrofit: Retrofit): NaverAuthApiService {
        return retrofit.create(NaverAuthApiService::class.java)
    }
}