pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Stable line (works great without Compose):
        id("com.android.application") version "8.4.2"
        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("com.google.dagger.hilt.android") version "2.51.1"
        // If you later need KSP: id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "LearnLog"
include(":app")
