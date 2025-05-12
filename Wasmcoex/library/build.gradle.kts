plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.example.wasmcoex"
version = "1.0.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.11")
    testImplementation(kotlin("test"))
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "wasmcoex-library"
            version = project.version.toString()
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
