plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "io.github.kevin321happy.httpdeck"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
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
    api(project(":httpdeck-core"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}