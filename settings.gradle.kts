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
        maven { url = uri("https://jitpack.io") }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("navigation", "2.7.3")
            version("hilt", "2.47")

            library("navigation-fragment", "androidx.navigation", "navigation-fragment-ktx")
                .versionRef("navigation")
            library("navigation-ui", "androidx.navigation", "navigation-ui-ktx")
                .versionRef("navigation")

            library("hilt-android", "com.google.dagger", "hilt-android")
                .versionRef("hilt")
            library("hilt-compiler", "com.google.dagger", "hilt-compiler")
                .versionRef("hilt")
        }
    }
}

rootProject.name = "LearnLog"
include(":app")
