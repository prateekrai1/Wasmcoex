pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.1.20"
        id("com.gradle.plugin-publish") version "1.2.1"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

gradle.beforeProject {
    afterEvaluate {
        tasks.findByName("preBuild")?.dependsOn("installEmscripten")
    }
}

rootProject.name = "Wasmcoex"
include(":plugin", ":library")
project(":plugin").projectDir = file("plugin")
project(":library").projectDir = file("library")
