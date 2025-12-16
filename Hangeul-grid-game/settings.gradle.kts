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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OSS-project"

// 메인 앱
include(":app")

// 미니게임 / 라이브러리 모듈들
include(":Hangeul-grid-game")
include(":Hangeulstudy")
include(":IconWordGame-main")
include(":ShootingGame")
