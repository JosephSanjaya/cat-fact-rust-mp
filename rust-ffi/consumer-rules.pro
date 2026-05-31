# Consumer ProGuard rules for rust-ffi module

# Keep all UniFFI generated classes
-keep class uniffi.catfact.** { *; }

# Keep JNA classes
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure {
    *;
}
