# ========================================
# YumeBox Extension ProGuard Configuration
# ========================================

# ========================================
# Native Methods & Android Core
# ========================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========================================
# Kotlin Core
# ========================================
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# ========================================
# Compose
# ========================================
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Preserve Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep interface androidx.compose.runtime.** { *; }

# Preserve Compose UI
-keep class androidx.compose.ui.** { *; }
-keep interface androidx.compose.ui.** { *; }

# Preserve Compose foundation
-keep class androidx.compose.foundation.** { *; }
-keep interface androidx.compose.foundation.** { *; }

# ========================================
# Javet (JavaScript Engine)
# ========================================
-keep class com.caoccao.javet.** { *; }
-keep interface com.caoccao.javet.** { *; }

# Suppress warnings for JMX classes not available on Android
-dontwarn java.lang.management.**
-dontwarn javax.management.**

# ========================================
# Extension-Specific
# ========================================
-keep class plus.yumeyuka.yumebox.extension.** { *; }
-keep interface plus.yumeyuka.yumebox.extension.** { *; }

# ========================================
# Optimization Configuration
# ========================================
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes LineNumberTable, SourceFile
