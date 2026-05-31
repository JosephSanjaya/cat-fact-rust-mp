#!/bin/bash
# Setup script for multiplatform Rust development

set -e

echo "🦀 Setting up Rust multiplatform development environment..."

# Check if Rust is installed
if ! command -v rustc &> /dev/null; then
    echo "❌ Rust is not installed. Please install from https://rustup.rs"
    exit 1
fi

echo "✅ Rust $(rustc --version) found"

# Install required targets
echo ""
echo "📦 Installing cross-compilation targets..."

targets=(
    "aarch64-apple-ios"
    "aarch64-apple-ios-sim"
    "x86_64-apple-ios"
    "aarch64-linux-android"
    "armv7-linux-androideabi"
    "i686-linux-android"
    "x86_64-linux-android"
    "wasm32-unknown-unknown"
)

for target in "${targets[@]}"; do
    echo "  Installing $target..."
    rustup target add "$target" || echo "  ⚠️  Failed to install $target (may require additional setup)"
done

# Install useful tools
echo ""
echo "🔧 Installing development tools..."

tools=(
    "cargo-ndk"
    "cargo-watch"
    "cargo-edit"
)

for tool in "${tools[@]}"; do
    if ! command -v "$tool" &> /dev/null; then
        echo "  Installing $tool..."
        cargo install "$tool" || echo "  ⚠️  Failed to install $tool"
    else
        echo "  ✅ $tool already installed"
    fi
done

# Check for Android NDK (optional)
echo ""
if [ -n "$ANDROID_NDK_HOME" ]; then
    echo "✅ Android NDK found at: $ANDROID_NDK_HOME"
else
    echo "⚠️  ANDROID_NDK_HOME not set. Android builds will not work."
    echo "   Download from: https://developer.android.com/ndk/downloads"
fi

# Check for Xcode (macOS only)
if [[ "$OSTYPE" == "darwin"* ]]; then
    if command -v xcodebuild &> /dev/null; then
        echo "✅ Xcode found: $(xcodebuild -version | head -n 1)"
    else
        echo "⚠️  Xcode not found. iOS builds will not work."
    fi
fi

echo ""
echo "🎉 Setup complete! Next steps:"
echo ""
echo "  1. Build the project:     cargo build"
echo "  2. Run tests:             cargo test"
echo "  3. Run the CLI example:   cargo run"
echo "  4. Build for Android:     cargo ndk -t arm64-v8a build --release -p catfact-ffi"
echo "  5. Build for iOS:         cargo build --release --target aarch64-apple-ios -p catfact-ffi"
echo ""
