# ============================================================
# qlmd3 - ProGuard / R8 混淆规则
# ============================================================

# 通用
-keepattributes Signature
-keepattributes *Annotation*

# ============================================================
# Gson 数据模型 - 包名修正
# 混淆后 Gson 需通过反射访问字段，必须保留所有数据类
# ============================================================
-keep class me.doujiang.app.data.model.** { *; }
-keepclassmembers class me.doujiang.app.data.model.** { *; }

# 保留 @SerializedName 注解（R8 混淆时 Gson 需要）
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================
# Retrofit
# ============================================================
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 保留 API 接口和 Retrofit 客户端（不会被用到但需防止混淆）
-keep class me.doujiang.app.data.api.** { *; }

# 保留 Repository
-keep class me.doujiang.app.data.repository.** { *; }

# 保留 Application 入口
-keep class me.doujiang.app.QingLongApp { *; }
-keep class me.doujiang.app.MainActivity { *; }

# Gson 泛型响应包装（如 QingLongResponse<T>）
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Gson @SerializedName 不混淆
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================
# OkHttp / Okio
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**

# ============================================================
# Kotlin / Coroutines
# ============================================================
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# 保留 Kotlin 协程 suspend 函数
-keepclassmembers class * {
    *** suspend*(...);
}

# ============================================================
# Jetpack Compose
# ============================================================
-dontwarn androidx.compose.**
