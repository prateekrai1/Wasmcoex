plugins {
    kotlin("jvm") version "1.9.24"
    id("maven-publish")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.example.wasmcoex"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
}

gradlePlugin {
    plugins {
        create("wasmcoexPlugin") {
            id = "com.example.wasmcoex"
            implementationClass = "com.example.wasmcoex.WasmcoexPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "wasmcoex-gradle-plugin"
            version = project.version.toString()
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}
