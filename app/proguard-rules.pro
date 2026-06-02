# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ─── Compose ───
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# ─── Kotlin ───
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ─── Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── 项目实体（Room/序列化用反射）───
-keep class com.example.funlife.data.model.** { *; }
-keep class com.example.funlife.data.entity.** { *; }

# ─── 数据备份/恢复（Gson 反射读写字段名，必须保留）───
-keep class com.example.funlife.utils.DataBackupManager$* { *; }
-keep class com.example.funlife.utils.DataBackupManager$BackupBundle { *; }
-keep class com.example.funlife.utils.DataBackupManager$ImportResult { *; }

# ─── Gson 自身 ───
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepattributes Signature
-keepattributes *Annotation*

# ─── 剥离 release 构建中的 Log（减小包体 + 消除日志泄漏）───
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# ─── 防止反射调用的 ViewModel 构造函数被混淆 ───
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }

# ─── VIP 网络模型（Gson 序列化字段必须保留）───
-keep class com.example.funlife.vip.VipCertificate { *; }
-keep class com.example.funlife.vip.VipCertResponse { *; }
-keepclassmembers class com.example.funlife.vip.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── OkHttp / Okio（避免运行时反射缺类警告）───
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# ─── 安全相关：保留关键校验逻辑的方法不被内联（让 hook 难度上升）───
-keepclassmembers class com.example.funlife.security.MonotonicClock {
    public *;
}
-keepclassmembers class com.example.funlife.vip.VipCertificateValidator {
    public *;
}

# ─── release 包剥离 error 级日志（极少数关键错误仍打到系统日志）───
-assumenosideeffects class android.util.Log {
    public static *** e(...);
}
