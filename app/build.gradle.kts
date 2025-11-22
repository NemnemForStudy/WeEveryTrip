import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}

android {
    namespace = "com.example.travelapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.travelapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        val kakaoKey = properties.getProperty("kakao.native.app.key") ?: ""
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")

        val naverClientId = properties.getProperty("naver.client.id") ?: ""
        buildConfigField("String", "NAVER_CLIENT_ID", "\"$naverClientId\"")

        val naverClientSecret = properties.getProperty("naver.client.secret") ?: ""
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"$naverClientSecret\"")

        val googleWebClientId = properties.getProperty("google.web.client.id") ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        // 카카오 콜백위해 꼭 있어야함.
        manifestPlaceholders["kakao_app_key"] = "kakao${kakaoKey}"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    // Test and Debug dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // 카카오 로그인
    implementation("com.kakao.sdk:v2-all:2.20.1")

    // Naver Login
    implementation("com.navercorp.nid:oauth:5.10.0")
//    implementation("androidx.browser:browser:1.7.0")

    // 네비게이션 라이브러리 추가
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 구글 로그인
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Hilt Navigation Compose (추가)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // JSON 파싱을 위한 GSON 컨버터
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // HTTP 요청 로깅 (디버그용)

}