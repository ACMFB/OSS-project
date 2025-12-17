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
    }
}


rootProject.name = "OSS-project"
include(":app")
include(":Hangeulstudy")
include(":ShootingGame")
include(":IconWordGame-main")
include(":Hangeul-grid-game")



