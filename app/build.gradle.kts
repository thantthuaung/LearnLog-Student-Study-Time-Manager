plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

android {
    namespace = "com.example.learnlog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.learnlog"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation (updated)
    val navVersion = "2.7.3"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
    implementation("androidx.navigation:navigation-runtime-ktx:$navVersion")

    // Room
    val roomVersion = "2.6.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Fragment
    implementation(libs.androidx.fragment.ktx)

    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ThreeTenABP for date/time handling on older Android versions
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")

    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}