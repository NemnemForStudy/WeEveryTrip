pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // --- 여기에도 카카오 저장소를 추가해 일관성을 맞춰줍니다. ---
        maven { url = uri("https://devrepo.kakao.com/nexus/repository/maven-public/") }
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // --- 이전에 추가한 이 부분은 올바르게 유지합니다. ---
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}

rootProject.name = "TravelApp"
include(":app")
