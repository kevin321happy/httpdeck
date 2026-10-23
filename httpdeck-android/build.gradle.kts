import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

group = "io.github.kevin321happy"
version = "0.1.0-alpha.3"

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

    publishing {
        singleVariant("release")
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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "httpdeck-android"
            }
        }
        repositories {
            maven {
                url = uri(rootProject.layout.projectDirectory.dir("docs/maven"))
            }
        }
    }
}