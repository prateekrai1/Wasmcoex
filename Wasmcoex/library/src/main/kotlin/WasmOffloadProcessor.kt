package com.example.wasmcoex

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class WasmOffloadProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(WasmOffload::class.qualifiedName!!)
        val validSymbols = symbols.filterIsInstance<KSFunctionDeclaration>()

        if (!validSymbols.iterator().hasNext()) return emptyList()

        val generatedClass = StringBuilder()
        generatedClass.appendLine("package com.example.wasmcoex")
        generatedClass.appendLine()
        generatedClass.appendLine("object WasmOffloadRegistry {")
        generatedClass.appendLine("val methods = listOf(")

        validSymbols.forEach { func ->
            val packageName = func.packageName.asString()
            val className = func.parentDeclaration?.simpleName?.asString()
            val methodName = func.simpleName.asString()

            if (className != null) {
                generatedClass.appendLine("\"$packageName.$className::$methodName\",")
            } else {
                logger.warn("Skipping function ${func.simpleName.getShortName()} because it is not inside a class.")
            }
        }

        generatedClass.appendLine("    )")
        generatedClass.appendLine("}")

        val file = environment.codeGenerator.createNewFile(
            Dependencies(false),
            "com.example.wasmcoex",
            "WasmOffloadRegistry"
        )

        file.writer().use {
            it.write(generatedClass.toString())
        }

        return emptyList()
    }
}

class WasmOffloadProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return WasmOffloadProcessor(environment)
    }
}
