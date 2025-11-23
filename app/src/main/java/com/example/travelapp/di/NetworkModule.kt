package com.example.travelapp.di

import com.example.travelapp.data.api.PostApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    // OKHttpClient를 제공함. 여기에 Interceptor 추가할 수 있음
    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
            .build()
    }

    // Retrofit 인스턴스 제공
// OkHttpClient와 BaseURL, Converter Factory 설정
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // TODO: 실제 API 서버 기본 URL로 변경해야함.
        // ex) "http://localhost:8080/"
        val BASE_URL = "http://10.0.2.2:3000" // 에뮬레이터에서 로컬 호스트 접근 시 사용
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // JSON 파싱 위한 GSON 컨버팅
            .build()
    }

    // PostApiService 인터페이스 구현체를 Retrofit을 통해 생성해 제공함.
    @Provides
    @Singleton
    fun providePostApiService(retrofit: Retrofit): PostApiService {
        return retrofit.create(PostApiService::class.java)
    }

}