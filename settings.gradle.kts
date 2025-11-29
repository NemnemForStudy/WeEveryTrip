pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 카카오 sdk
        maven("https://devrepo.kakao.com/nexus/content/groups/public/")
        // ️ 네이버 지도 SDK 저장소
        maven("https://repository.map.naver.com/archive/maven")
        // maven { url = uri("https://naver.jfrog.io/artifactory/map-sdk") }
    }
}

rootProject.name = "TravelApp"
include(":app")