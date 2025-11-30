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
    compileSdk = 36

    buildTypes {
        getByName("debug") {
            val baseUrl = localProperties.getProperty("BASE_URL") ?: "http://10.0.2.2:3000"
            val phoneBaseUrl = localProperties.getProperty("PHONE_BASE_URL", baseUrl)
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "PHONE_BASE_URL", "\"$phoneBaseUrl\"")
        }
        getByName("release") {
            val baseUrl = localProperties.getProperty("RELEASE_BASE_URL", "https://api.your-domain.com")
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

            isMinifyEnabled = false
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

    // 리소스 충돌 방지
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// JUnit 4를 사용하므로 JUnit Platform 설정은 제거합니다.

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

    // Test dependencies (JUnit 4)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")

    // AndroidTest dependencies (JUnit 4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation("org.mockito:mockito-android:5.7.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // ⭐️ [핵심 해결책] kotlin-reflect 버전을 메인과 테스트 양쪽에 모두 강제로 2.0.21로 고정합니다.
    // 이 설정이 이전 충돌 에러를 해결합니다.
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    androidTestImplementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    // Third Party Libraries
    implementation("com.kakao.sdk:v2-all:2.20.1")
    implementation("com.navercorp.nid:oauth:5.10.0")
    implementation("androidx.navigation:navigation-compose")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.naver.maps:map-sdk:3.23.0")
    implementation("io.github.fornewid:naver-map-compose:1.7.2")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
}