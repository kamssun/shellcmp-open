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
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**

# ===== Koin =====
-keep class org.koin.** { *; }
-keepclassmembers class * { public <init>(...); }

# ===== MVIKotlin =====
-keep class com.arkivanov.mvikotlin.** { *; }
-keep class * implements com.arkivanov.mvikotlin.core.utils.JvmSerializable { *; }

# ===== Decompose =====
-keep class com.arkivanov.decompose.** { *; }
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
-keep class com.google.protobuf.** { *; }
-keep class **.proto.** { *; }
-keepclassmembers class **.protobuf.** {
    static <fields>;
}
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn io.agora.**

# ===== 通用 =====
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable