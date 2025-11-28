import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if(localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.travelapp"
    compileSdk {
        version = release(36)
    }

    buildTypes {
        getByName("debug") {
            // local.properties에서 BASE_URL 값을 읽어오거나, 없으면 기본값을 사용합니다.
            val baseUrl = localProperties.getProperty("BASE_URL")
            val phoneBaseUrl = localProperties.getProperty("PHONE_BASE_URL", baseUrl)
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "PHONE_BASE_URL", "\"$phoneBaseUrl\"")
        }
        getByName("release") {
            // 릴리즈(출시) 빌드에서는 실제 배포된 서버 주소 사용
            // 지금은 없으니 임시주소
            val baseUrl = localProperties.getProperty("RELEASE_BASE_URL", "https://api.your-domain.com")
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

            isMinifyEnabled = false
            // 이 코드는 앱을 정식으로 배포(Release)할 때, 앱의 크기를 줄이고 코드를 알아보기 힘들게 만드는(난독화) 설정 파일들을 지정하는 명령어입니다.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
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
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // AndroidTest용 Mockito 추가
    androidTestImplementation("org.mockito:mockito-android:5.7.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    androidTestImplementation(kotlin("test"))
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // 카카오 로그인
    implementation("com.kakao.sdk:v2-all:2.20.1")

    // Naver Login
    implementation("com.navercorp.nid:oauth:5.10.0")
//    implementation("androidx.browser:browser:1.7.0")

    // 네비게이션 라이브러리 추가
        implementation("androidx.navigation:navigation-compose")

    // 구글 로그인
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Material 3와 호환되는 아이콘 라이브러리
        implementation("androidx.compose.material:material-icons-extended")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Hilt Navigation Compose (추가)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // JSON 파싱을 위한 GSON 컨버터
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // HTTP 요청 로깅 (디버그용)

    // Coil (이미지 로딩 라이브러리)
    implementation("io.coil-kt:coil-compose:2.6.0")
    testImplementation(kotlin("test"))

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}
