plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "io.github.kevin321happy.httpdeck.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.kevin321happy.httpdeck.sample"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":httpdeck-android"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}