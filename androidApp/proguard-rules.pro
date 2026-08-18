# ===== kotlinx.serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 保留项目中所有 @Serializable 类
-keep,includedescriptorclasses class com.example.archshowcase.**$$serializer { *; }
-keepclassmembers class com.example.archshowcase.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.archshowcase.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== Ktor =====
# keep-reason: Ktor client uses plugin pipelines and platform engines; keep broad until consumer rules are verified against release runtime flows.
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**

# ===== Koin =====
# keep-reason: Koin definitions are created through DSL/lambdas across shared modules; keep broad until constructor/reflection assumptions are audited.
-keep class org.koin.** { *; }
-keepclassmembers class * { public <init>(...); }

# ===== MVIKotlin =====
# keep-reason: MVIKotlin state/time-travel paths serialize framework types and stores across debug/replay tooling.
-keep class com.arkivanov.mvikotlin.** { *; }
-keep class * implements com.arkivanov.mvikotlin.core.utils.JvmSerializable { *; }

# ===== Decompose =====
# keep-reason: Decompose navigation and lifecycle objects participate in retained component state and route restoration.
-keep class com.arkivanov.decompose.** { *; }
# keep-reason: Essenty lifecycle/state-keeper objects are retained by Decompose component contexts.
-keep class com.arkivanov.essenty.** { *; }

# ===== Coil =====
-dontwarn coil3.**

# ===== OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**

# ===== Gson（IM SDK / Retrofit 传递依赖）=====
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# keep-reason: Gson internals are used by transitive adapters and reflective TypeToken deserialization.
-keep class com.google.gson.internal.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# R8 full mode: 保留 TypeToken 泛型反射
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ===== Retrofit（SDK 传递依赖）=====
-dontwarn retrofit2.**
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# ===== IM SDK =====
# Proprietary IM SDK keep rules removed for open-source showcase.
# Add your IM SDK ProGuard rules here.
# keep-reason: Protobuf generated schemas are used by the showcase serialization paths.
-keep class com.google.protobuf.** { *; }
# keep-reason: Generated proto packages are deserialized by network payload paths.
-keep class **.proto.** { *; }
-keepclassmembers class **.protobuf.** {
    static <fields>;
}
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn io.agora.**

# ===== 通用 =====
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
