#!/bin/bash
# Build script for Android targets

set -e

if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "❌ ANDROID_NDK_HOME is not set"
    echo "Please set it to your Android NDK installation path"
    exit 1
fi

echo "🤖 Building for Android..."

# Android targets
TARGETS=(
    "aarch64-linux-android"    # arm64-v8a
    "armv7-linux-androideabi"  # armeabi-v7a
    "i686-linux-android"       # x86
    "x86_64-linux-android"     # x86_64
)

OUTPUT_DIR="./android/jniLibs"

for target in "${TARGETS[@]}"; do
    echo "Building for $target..."
    cargo build --release --target "$target" -p catfact-ffi
done

echo ""
echo "✅ Android builds complete!"
echo ""
echo "Libraries are in: target/{target}/release/libcatfact.so"
echo ""
echo "To generate Kotlin bindings, run:"
echo "  cargo run --bin uniffi-bindgen generate bindings/catfact-ffi/src/catfact.udl --language kotlin --out-dir ./android/src/main/kotlin"
