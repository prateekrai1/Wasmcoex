// File: buildSrc/src/main/kotlin/com/example/wasmcoex/WasmcoexPlugin.kt
package com.example.wasmcoex

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.IOException

open class WasmcoexExtension {
    var cppSourceDir: String = "src/main/cpp"
    var wasmOutputDir: String = "/"
}

class WasmcoexPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("wasmcoex", WasmcoexExtension::class.java)

        project.tasks.register("compileCppToWasm") { task ->
            task.group = "wasmcoex"
            task.description = "Compiles pure C++ files (excluding JNI) to WASM using em++"

            task.doLast {
                checkOrInstallEmscripten(project)

                val cppDir = project.file(extension.cppSourceDir)
                val wasmDir = project.file(extension.wasmOutputDir)

                println("🔍 Scanning C++ source directory: ${cppDir.absolutePath}")

                if (!cppDir.exists()) {
                    println("❌ C++ source directory not found!")
                    return@doLast
                }

                if (!wasmDir.exists()) {
                    println("📂 Creating wasm output directory at ${wasmDir.absolutePath}")
                    wasmDir.mkdirs()
                }

                val allCppFiles = cppDir.walkTopDown()
                    .filter { it.isFile && it.extension == "cpp" }
                    .toList()

                println("📦 Found ${allCppFiles.size} .cpp files:")

                var compiledCount = 0
                allCppFiles.forEach { file ->
                    val isPure = isPureCpp(file)
                    println("   - ${file.name} => ${if (isPure) "✅ compiling" else "❌ skipped (JNI)"}")

                    if (isPure) {
                        val outputWasm = File(wasmDir, "${file.nameWithoutExtension}.wasm")
                        println("⚙️ Compiling ${file.name} ➔ ${outputWasm.name}")

                        try {
                            project.exec { spec ->
                                spec.commandLine(
                                    "em++", file.absolutePath,
                                    "-o", outputWasm.absolutePath,
                                    "-s", "WASM=1",
                                    "-O3"
                                )
                            }
                            compiledCount++
                        } catch (e: Exception) {
                            println("❌ Failed to compile ${file.name}: ${e.message}")
                        }
                    }
                }

                println("✅ WASM compilation complete. Total compiled: $compiledCount/${allCppFiles.size}")
                println("📁 Output directory: ${wasmDir.absolutePath}")
            }
        }
    }

    private fun isPureCpp(file: File): Boolean {
        val content = file.readText()
        return !content.contains("JNIEXPORT")
    }

    private fun checkOrInstallEmscripten(project: Project) {
        try {
            val result = ProcessBuilder("em++", "--version")
                .redirectErrorStream(true)
                .start()
                .waitFor()

            if (result == 0) {
                println("✅ Emscripten (em++) found in PATH.")
                return
            }
        } catch (e: IOException) {
            println("❌ em++ compiler not found.")
        }

        println("\n🔧 Installing Emscripten SDK...")

        val homeDir = System.getProperty("user.home")
        val emsdkDir = File(homeDir, "emsdk")

        if (!emsdkDir.exists()) {
            println("📥 Cloning emsdk...")
            project.exec {
                it.commandLine("git", "clone", "https://github.com/emscripten-core/emsdk.git", emsdkDir.absolutePath)
            }
        }

        println("📦 Installing latest emsdk...")
        project.exec {
            it.workingDir = emsdkDir
            it.commandLine("./emsdk", "install", "latest")
        }

        println("🚀 Activating latest emsdk...")
        project.exec {
            it.workingDir = emsdkDir
            it.commandLine("./emsdk", "activate", "latest")
        }

        println("✅ Emscripten installed. Please manually run:")
        println("   source ${emsdkDir.absolutePath}/emsdk_env.sh")
        println("   Then retry the Gradle build.")

        throw RuntimeException("❗ Emscripten installed, but please set PATH manually before continuing.")
    }
}

