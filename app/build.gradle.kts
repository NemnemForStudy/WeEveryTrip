import java.util.Properties

plugins {
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if(localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.travelapp"
    compileSdk = 36

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if(localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    buildTypes {
        getByName("debug") {
            // 코틀린 문법이므로 반드시 괄호()를 써야 합니다.
            buildConfigField("String", "BASE_URL", "\"https://modutrip.onrender.com/\"")
        }
        getByName("release") {
            buildConfigField("String", "BASE_URL", "\"https://modutrip.onrender.com/\"")
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
        if (project.rootProject.file("local.properties").exists()) {
            properties.load(project.rootProject.file("local.properties").inputStream())
        }

        val kakaoKey = properties.getProperty("kakao.native.app.key") ?: ""
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoKey\"")

        val naverClientId = properties.getProperty("naver.client.id") ?: ""
        buildConfigField("String", "NAVER_CLIENT_ID", "\"$naverClientId\"")

        val naverClientSecret = properties.getProperty("naver.client.secret") ?: ""
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"$naverClientSecret\"")

        val googleWebClientId = properties.getProperty("google.web.client.id") ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        manifestPlaceholders["kakao_app_key"] = "kakao${kakaoKey}"

        val naverMapClientId = properties.getProperty("naver.map.client.id") ?: ""
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"$naverMapClientId\"")
        // 메니페스트(XML)용 변수 연결
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = naverMapClientId

        val kakaoNativeAppKey = properties.getProperty("kakao.native.app.key") ?: ""
        buildConfigField("String","KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey

        val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: "\"\""
        buildConfigField("String", "SUPABASE_URL", supabaseUrl)
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ⭐️ [필수 추가] Hilt 컴파일러가 에러 타입을 올바르게 수정하도록 설정 (NPE 방지)
kapt {
    correctErrorTypes = true
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

    // --- Hilt (의존성 주입) ---
    implementation(libs.hilt.android)
    implementation(libs.androidx.ui)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
//    implementation(libs.firebase.firestore.ktx)
    kapt(libs.hilt.compiler)
    kaptAndroidTest(libs.hilt.compiler)

    // --- Test (JUnit 4) ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("io.mockk:mockk:1.13.8")

    // --- Android Test (JUnit 4) ---
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation("org.mockito:mockito-android:5.7.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // [유지] kotlin-reflect 충돌 방지용 강제 버전 지정
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    androidTestImplementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    // Hilt Testing 라이브러리 (kaptAndroidTest가 동작하려면 이 라이브러리가 꼭 필요합니다!)
    // 버전은 사용 중인 Hilt 버전에 맞춰야 합니다. (보통 2.48 ~ 2.51.1)
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")

    // --- Third Party ---
    implementation("com.kakao.sdk:v2-all:2.20.1")
    implementation("com.navercorp.nid:oauth:5.10.0")
    implementation("androidx.navigation:navigation-compose")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt Navigation
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Map & Location
    implementation("com.naver.maps:map-sdk:3.23.0")
    implementation("io.github.fornewid:naver-map-compose:1.7.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // 1. Firebase BoM을 가장 먼저 선언 (버전 관리자)
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))

    // 2. 나머지 라이브러리 (버전 생략 가능)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // ... 기존에 있던 hilt, navigation, google-auth 등 라이브러리들 ...
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)

    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // build.gradle (app) 에 추가
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("dev.shreyaspatil:capturable:2.1.0")
}