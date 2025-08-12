pluginManagement {
    repositories {
        gradlePluginPortal(); google(); mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "PointerOverlay"
include(":app")


// build.gradle.kts (root) — Full file (project root)
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}