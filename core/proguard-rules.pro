# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI bridge classes
-keep class plus.yumeyuka.yumebox.core.bridge.** { *; }
-keep class plus.yumeyuka.yumebox.core.Clash { *; }

# Keep data models for serialization
-keep class plus.yumeyuka.yumebox.core.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

-dontwarn kotlinx.serialization.**

