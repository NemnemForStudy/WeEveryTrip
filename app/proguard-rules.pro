# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ----------------------------------------------------------------------------
# 1. [기본] 코드 최적화 및 디버깅 정보 설정
# ----------------------------------------------------------------------------
# 제네릭(<T>), 어노테이션 정보 등을 유지합니다. (Retrofit/Hilt 필수)
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes SourceFile,LineNumberTable

# ----------------------------------------------------------------------------
# 2. [필수] Retrofit & OkHttp (서버 통신)
# ----------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
# Retrofit이 사용하는 어노테이션(@GET, @POST 등)이 붙은 메소드 보호
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# ----------------------------------------------------------------------------
# 3. [핵심] 하누님 앱의 데이터 모델 보호 (400 에러 해결)
# ----------------------------------------------------------------------------
# 서버로 보내는 데이터 클래스(DTO)의 변수명이 'a', 'b'로 바뀌지 않게 통째로 보호합니다.
# 하누님의 패키지 구조에 맞춰 data, domain, model, ui 패키지 등을 보호합니다.
-keep class com.nemnem.travelapp.data.** { *; }
-keep class com.nemnem.travelapp.domain.** { *; }
-keep class com.nemnem.travelapp.model.** { *; }
# 혹시 데이터 클래스가 최상위에 있거나 다른 곳에 있을 경우를 대비해 DTO/Entity 이름이 들어간 클래스 보호
-keep class **.dto.** { *; }
-keep class **.entity.** { *; }

# GSON/Serialization 어노테이션이 붙은 필드는 무조건 보호
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# ----------------------------------------------------------------------------
# 4. Kakao SDK (크래시 방지)
# ----------------------------------------------------------------------------
-keep class com.kakao.sdk.**.model.** { *; }
-keep class com.kakao.sdk.** { *; }
-keep interface com.kakao.sdk.** { *; }
-dontwarn com.kakao.sdk.**

# ----------------------------------------------------------------------------
# 5. Naver Map SDK
# ----------------------------------------------------------------------------
-keep class com.naver.maps.** { *; }
-keep interface com.naver.maps.** { *; }

# ----------------------------------------------------------------------------
# 6. Hilt & Dagger (의존성 주입)
# ----------------------------------------------------------------------------
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class androidx.hilt.** { *; }
-keep class com.google.dagger.** { *; }
-keep class javax.inject.** { *; }

# ----------------------------------------------------------------------------
# 7. Google Login & Firebase Auth (로그인 문제 해결)
# ----------------------------------------------------------------------------
# Google Identity & Credentials
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# Google Play Services Auth
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
# 1. Retrofit & OkHttp (데이터 통신 필수)
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# 2. Kakao SDK (현재 크래시 해결의 핵심)
-keep class com.kakao.sdk.**.model.** { *; }
-keep class com.kakao.sdk.** { *; }
-keep interface com.kakao.sdk.** { *; }
-dontwarn com.kakao.sdk.**

# 3. Naver Map SDK (네이버 지도 사용 시 필수)
-keep class com.naver.maps.** { *; }
-keep interface com.naver.maps.** { *; }

# 4. Hilt & Dagger (의존성 주입 보호)
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class androidx.hilt.** { *; }
-keep class com.google.dagger.** { *; }

# 5. Data Classes (GSON/Serialization 사용 시 변수명 보존)
# API 통신에 사용하는 데이터 모델 클래스들이 모인 패키지 경로를 적어주면 더 좋습니다.
# 예: -keep class com.nemnem.travelapp.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Google Identity & Google ID (구글 로그인 필수)
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# AndroidX Credentials (최신 로그인 API)
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# Google Play Services Auth
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# Firebase Auth 관련 모델 보호
-keep class com.google.firebase.auth.** { *; }

-keep class com.nemnem.travelapp.** { *; }