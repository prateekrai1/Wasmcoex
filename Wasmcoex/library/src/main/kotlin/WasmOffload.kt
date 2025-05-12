package com.example.wasmcoex

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WasmOffload(val file: String = "")