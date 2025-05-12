//package com.example.wasmcoex
//
//import java.io.File
//import java.net.HttpURLConnection
//import java.net.URL
//
//object WasmExecutor {
//
//    private const val SERVER_URL = "http://10.0.2.2:8080"
//    private val uploadedFiles = mutableSetOf<String>()
//
//    /**
//     * Executes the specified WASM file remotely.
//     * Uploads if not already uploaded.
//     */
//    @JvmStatic
//    fun execute(wasmFile: String, args: List<String> = emptyList()): String {
//        if (!uploadedFiles.contains(wasmFile)) {
//            uploadWasm(wasmFile)
//            uploadedFiles.add(wasmFile)
//        }
//        return executeWasm(wasmFile, args)
//    }
//
//    private fun uploadWasm(wasmFileName: String) {
//        val wasmDir = File("src/main/assets")
//        val wasmFile = File(wasmDir, wasmFileName)
//
//        if (!wasmFile.exists()) {
//            throw RuntimeException("WASM file not found: ${wasmFile.absolutePath}")
//        }
//
//        val url = URL("$SERVER_URL/upload")
//        val boundary = "Boundary-${System.currentTimeMillis()}"
//
//        val connection = (url.openConnection() as HttpURLConnection).apply {
//            doOutput = true
//            requestMethod = "POST"
//            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
//        }
//
//        connection.outputStream.use { output ->
//            output.writer().use { writer ->
//                writer.append("--$boundary\r\n")
//                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"${wasmFile.name}\"\r\n")
//                writer.append("Content-Type: application/wasm\r\n\r\n")
//                writer.flush()
//                output.write(wasmFile.readBytes())
//                output.flush()
//                writer.append("\r\n--$boundary--\r\n")
//                writer.flush()
//            }
//        }
//
//        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
//            throw RuntimeException("Failed to upload WASM: HTTP ${connection.responseCode}")
//        }
//
//        println("✅ Uploaded ${wasmFile.name} successfully!")
//    }
//
//    private fun executeWasm(wasmFileName: String, args: List<String>): String {
//        val queryArgs = args.joinToString("&") { "arg=$it" }
//        val url = URL("$SERVER_URL/execute?file=$wasmFileName&$queryArgs")
//
//        val connection = (url.openConnection() as HttpURLConnection).apply {
//            requestMethod = "GET"
//        }
//
//        val output = connection.inputStream.bufferedReader().readText()
//        println("✅ Executed ${wasmFileName}, output: $output")
//
//        return output
//    }
//}

package com.example.wasmcoex

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object WasmExecutor {
  private const val SERVER_URL = "http://127.0.0.1:8080"
  private val uploaded = mutableSetOf<String>()

  // Directory on the emulator you’ll push your .wasm into:
  private val externalDir = File("/sdcard/wasm_files")

  /** JNI calls this: execute("matrix_factorization.wasm", args) */
  @JvmStatic
  fun execute(wasmName: String, args: List<String>): String {
    if (uploaded.add(wasmName)) {
      uploadWasm(wasmName)
    }
    return callExecute(wasmName, args)
  }

  private fun uploadWasm(wasmName: String) {
    // 1️⃣ Try loading from classpath resources
    val tmpFile: File = Thread.currentThread().contextClassLoader
      .getResourceAsStream(wasmName)
      ?.use { input ->
        File.createTempFile("wasm_", "_$wasmName").apply {
          outputStream().use { out -> input.copyTo(out) }   // copy bytes INTO out
          deleteOnExit()
        }
      }
      // 2️⃣ Fallback to /sdcard/wasm_files
      ?: run {
        val f = File(externalDir, wasmName)
        if (!f.exists()) {
          throw RuntimeException("WASM not found in resources or /sdcard/wasm_files: $wasmName")
        }
        f
      }

    // 3️⃣ POST it
    val conn = (URL("$SERVER_URL/upload").openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      doOutput     = true
      setRequestProperty("Content-Type", "application/wasm")
    }
    // ⬇️ Copy from the file’s InputStream INTO the connection’s OutputStream
    tmpFile.inputStream().use { input ->
      conn.outputStream.use { output ->
        input.copyTo(output)   // <— correct direction
      }
    }
    if (conn.responseCode != HttpURLConnection.HTTP_OK) {
      throw RuntimeException("Upload failed: HTTP ${conn.responseCode}")
    }
    conn.disconnect()
  }

  private fun callExecute(wasmName: String, args: List<String>): String {
    val qs = buildString {
      append("?file=").append(wasmName)
      args.forEach { append("&arg=").append(it) }
    }
    val conn = (URL("$SERVER_URL/execute$qs").openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
    }
    return conn.inputStream.bufferedReader().use { it.readText() }
      .also { conn.disconnect() }
  }
}

