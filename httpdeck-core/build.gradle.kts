import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "io.github.kevin321happy"
version = "0.1.0-alpha.3"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "httpdeck-core"
        }
    }
    repositories {
        maven {
            url = uri(rootProject.layout.projectDirectory.dir("docs/maven"))
        }
    }
}