#!/bin/bash
# Quick setup script for Rust-Android integration

set -e

echo "🦀 Cat Fact Android - Setup Script"
echo "=================================="
echo ""

# Check if Rust is installed
if ! command -v cargo &> /dev/null; then
    echo "❌ Rust is not installed"
    echo ""
    echo "Please install Rust from: https://rustup.rs/"
    echo "Run: curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
    exit 1
fi

echo "✅ Rust is installed: $(cargo --version)"
echo ""

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  Warning: ANDROID_HOME or ANDROID_SDK_ROOT not set"
    echo "   Android Studio should set this automatically"
    echo ""
fi

# Check if NDK is configured
if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "⚠️  Warning: ANDROID_NDK_HOME not set"
    echo "   The build will try to find NDK from local.properties"
    echo "   You can set it manually:"
    echo "   export ANDROID_NDK_HOME=/path/to/android/sdk/ndk/27.2.12479018"
    echo ""
fi

# Install Rust targets for Android
echo "📦 Installing Rust targets for Android..."
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add i686-linux-android
rustup target add x86_64-linux-android

echo ""
echo "✅ Rust targets installed"
echo ""

# Install uniffi-bindgen (optional, Gradle will install it if needed)
echo "📦 Checking uniffi-bindgen..."
if command -v uniffi-bindgen &> /dev/null; then
    echo "✅ uniffi-bindgen is already installed: $(uniffi-bindgen --version)"
else
    echo "⚠️  uniffi-bindgen not found (Gradle will install it automatically)"
fi

echo ""
echo "🎉 Setup complete!"
echo ""
echo "Next steps:"
echo "1. Configure NDK path in local.properties (if not already set)"
echo "2. Run: ./gradlew build"
echo "3. Run: ./gradlew installDebug"
echo ""
echo "For more information, see:"
echo "- README.md"
echo "- INTEGRATION_GUIDE.md"
echo ""
