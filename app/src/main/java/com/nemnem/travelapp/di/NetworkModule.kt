package com.nemnem.travelapp.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.android.gms.auth.api.Auth
import com.kakao.sdk.auth.AuthApi
import com.nemnem.travelapp.BuildConfig
import com.nemnem.travelapp.data.api.AuthApiService
import com.nemnem.travelapp.data.api.AuthAuthenticator
import com.nemnem.travelapp.data.api.AuthInterceptor
import com.nemnem.travelapp.data.api.CommentApiService
import com.nemnem.travelapp.data.api.InquiryApiService
import com.nemnem.travelapp.data.api.NaverAuthApiService
import com.nemnem.travelapp.data.api.PostApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
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
            .connectTimeout(30, TimeUnit.SECONDS) // 연결 대기 시간 30초
            .readTimeout(30, TimeUnit.SECONDS)    // 응답 읽기 시간 30초
            .writeTimeout(30, TimeUnit.SECONDS)   // 요청 쓰기 시간 30초
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideChuckerInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        return ChuckerInterceptor.Builder(context)
            .collector(ChuckerCollector(context))
            .maxContentLength(250000L)
            .redactHeaders(emptySet())
            .alwaysReadResponseBody(true)
            .build()
    }

    /**
     * [수정 포인트 1] 인증 전용 Retrofit 추가 (DRY 원칙)
     * 로그인, 회원가입, 토큰 갱신 API는 'AuthInterceptor'가 붙지 않은 클라이언트를 써야 합니다.
     * 그래야 토큰 갱신 요청에 만료된 토큰이 중복으로 붙는 '간섭'을 막을 수 있어요.
     */
    @Provides
    @Singleton
    @Named("AuthRefreshRetrofit")
    fun provideAuthRefreshRetrofit(
        @Named("DefaultOkHttpClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 인증 토큰(JWT)이 포함된 클라이언트 (게시글 작성, 조회 등)
    // AuthInterceptor가 여기서 자동으로 헤더를 붙여줍니다.
    @Provides
    @Singleton
    @Named("AuthOkHttpClient")
    fun provideAuthOkHttpClient(
        authInterceptor: AuthInterceptor,
        authAuthenticator: AuthAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // 문의하기 메일 발송을 고려해 60초 추천
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chuckerInterceptor)
            .authenticator(authAuthenticator)
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
        // 빌드 설정에 따라 자동으로 주소가 바뀜
        val baseUrl = BuildConfig.BASE_URL

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

    /**
     * [갱신 전용 서비스] 로그인을 하거나 토큰을 새로 고칠 때 사용합니다.
     * 이 녀석은 '토큰 없이' 혹은 '리프레시 토큰만' 가지고 서버에 가야 하므로
     * 아무런 헤더 간섭이 없는 'AuthRefreshRetrofit'을 사용합니다.
     */
    @Provides
    @Singleton
    @Named("RefreshApiService")
    fun provideRefreshApiService(@Named("AuthRefreshRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    /**
     * [인증 정보 서비스] 내 프로필 조회, 회원 정보 수정 등 '내 토큰'이 필요한 경우 사용합니다.
     * 이 녀석은 자동으로 토큰을 붙여주고, 만료 시 갱신까지 해주는 'AppRetrofit'을 사용합니다.
     */
    @Provides
    @Singleton
    @Named("ProfileApiService")
    fun provideProfileApiService(@Named("AppRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
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
    /*@Provides
    @Singleton
    fun provideAuthApiService(@Named("AuthRefreshRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }*/

    // NaverAuthApiService는 NaverRetrofit 사용하도록 새로 추가
    // 네이버 인증 API
    @Provides
    @Singleton
    fun provideNaverAuthApiService(@Named("NaverRetrofit") retrofit: Retrofit): NaverAuthApiService {
        return retrofit.create(NaverAuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCommentApiService(@Named("AppRetrofit") retrofit: Retrofit): CommentApiService {
        return retrofit.create(CommentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideInquiryApiService(@Named("AppRetrofit") retrofit: Retrofit): InquiryApiService {
        return retrofit.create(InquiryApiService::class.java)
    }

    // 로그인, 회원가입, 토큰 갱신용 (토큰이 필요 없는 용도)
    @Provides
    @Singleton
    @Named("AuthNoToken")
    fun provideAuthApiServiceNoToken(@Named("AuthRefreshRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    // 프로필 수정 등 (토큰이 필요한 용도)
    @Provides
    @Singleton
    @Named("AuthWithToken")
    fun provideAuthApiServiceWithToken(@Named("AppRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
}