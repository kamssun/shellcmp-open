package com.example.archshowcase.replayable.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertTrue

class VfResolvableProcessorTest {

    @Test
    fun generatedRegistryFallsBackToBaseStoreNameForInstanceStores() {
        val codeGenerator = CapturingCodeGenerator()
        val processor = VfResolvableProcessor(codeGenerator, NoopLogger)

        processor.generatedResolversForTest += ResolverInfo(
            packageName = "com.example.archshowcase.presentation.chat.room",
            storeName = "ChatRoomStore",
            functionName = "resolveChatRoomStoreIntent",
            storeQualifiedName = "com.example.archshowcase.presentation.chat.room.ChatRoomStore"
        )

        processor.finish()

        val generated = codeGenerator.contentFor(
            "com.example.archshowcase.core.trace.verification",
            "GeneratedIntentResolverRegistry"
        )

        assertTrue(
            generated.contains("resolvers[vfIntent.store]"),
            "Generated registry should preserve exact store lookup:\n$generated"
        )
        assertTrue(
            generated.contains("resolvers[vfIntent.store.substringBefore(':')]"),
            "Generated registry should fall back from instance store names like ChatRoomStore:g1:\n$generated"
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val VfResolvableProcessor.generatedResolversForTest: MutableList<ResolverInfo>
        get() {
            val field = VfResolvableProcessor::class.java.getDeclaredField("generatedResolvers")
            field.isAccessible = true
            return field.get(this) as MutableList<ResolverInfo>
        }

    private class CapturingCodeGenerator : CodeGenerator {
        private val files = mutableMapOf<String, ByteArrayOutputStream>()

        override fun createNewFile(
            dependencies: Dependencies,
            packageName: String,
            fileName: String,
            extensionName: String
        ): OutputStream {
            return ByteArrayOutputStream().also { files[key(packageName, fileName, extensionName)] = it }
        }

        override fun createNewFileByPath(
            dependencies: Dependencies,
            path: String,
            extensionName: String
        ): OutputStream {
            return ByteArrayOutputStream().also { files["$path.$extensionName"] = it }
        }

        override fun associate(
            sources: List<KSFile>,
            packageName: String,
            fileName: String,
            extensionName: String
        ) = Unit

        override fun associateByPath(
            sources: List<KSFile>,
            path: String,
            extensionName: String
        ) = Unit

        override fun associateWithClasses(
            classes: List<KSClassDeclaration>,
            packageName: String,
            fileName: String,
            extensionName: String
        ) = Unit

        override val generatedFile: Collection<File> = emptyList()

        fun contentFor(packageName: String, fileName: String): String {
            return files.getValue(key(packageName, fileName, "kt")).toString(Charsets.UTF_8.name())
        }

        private fun key(packageName: String, fileName: String, extensionName: String) =
            "$packageName.$fileName.$extensionName"
    }

    private object NoopLogger : KSPLogger {
        override fun logging(message: String, symbol: KSNode?) = Unit
        override fun info(message: String, symbol: KSNode?) = Unit
        override fun warn(message: String, symbol: KSNode?) = Unit
        override fun error(message: String, symbol: KSNode?) = Unit
        override fun exception(e: Throwable) = Unit
    }
}
