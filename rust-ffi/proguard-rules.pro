# Rust FFI ProGuard rules

# Keep all UniFFI generated classes
-keep class uniffi.catfact.** { *; }

# Keep JNA classes (required for native library loading)
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure {
    *;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
