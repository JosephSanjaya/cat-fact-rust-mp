#!/bin/bash
set -e

# Navigate to the domain directory
CDIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$CDIR"

echo "🦀 Building WASM Bindings for Cat Fact API..."

# 1. Check if wasm-pack is installed
if ! command -v wasm-pack &> /dev/null; then
    echo "⚠️ wasm-pack is not installed. Attempting to install..."
    
    # Try to install via curl (fast binary download)
    if command -v curl &> /dev/null; then
        echo "📥 Downloading wasm-pack precompiled binary..."
        curl https://rustwasm.github.io/wasm-pack/installer/init.sh -sSf | sh
    else
        echo "🛠️ Compiling wasm-pack from source using cargo (this might take a while)..."
        cargo install wasm-pack
    fi
fi

# 2. Build the WASM module using wasm-pack
echo "📦 Running wasm-pack build..."
wasm-pack build bindings/catfact-wasm --target web --out-dir pkg

echo "✅ WASM package generated successfully in domain/bindings/catfact-wasm/pkg!"
