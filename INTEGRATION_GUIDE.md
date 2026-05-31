# Cross-Platform Rust Integration Guide (Android & iOS)

This guide explains how the shared Rust domain layer is integrated into both the native Android app (Kotlin) and native iOS app (Swift) using industry best practices.

---

## 🏗️ Architecture Overview

The integration follows a **clean three-layer architecture** separating core business logic from target user interfaces:

```
    ┌───────────────────────────┐     ┌───────────────────────────┐
    │    Android App (Compose)  │     │     iOS App (SwiftUI)     │  ← UI Layer
    └───────────────────────────┘     └───────────────────────────┘
                 │                                 │
                 ▼                                 ▼
    ┌───────────────────────────┐     ┌───────────────────────────┐
    │    Android Bridge Module  │     │    iOS Bridge Pipeline    │  ← Bridge Layer
    │       (:rust-ffi)         │     │  (Frameworks + bindings)  │
    └───────────────────────────┘     └───────────────────────────┘
                 \                                 /
                  \                               /
                   ▼                             ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                 Shared Rust Domain Layer                    │  ← Core Layer
    │  • Core logic (core)      • Network reqwest (networking)    │
    │  • FFI bindings (catfact-ffi) • UDL interface definitions   │
    └─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Shared Rust Crate Layer (`domain/`)

The shared domain logic is managed under a Cargo workspace:
- **`crates/core`**: Houses pure business logic, service components, and domain models.
- **`crates/networking`**: Implements HTTP interactions using `reqwest` and native platform verification modules (`rustls-platform-verifier`).
- **`bindings/catfact-ffi`**: Employs UniFFI scaffolding macros and a `src/catfact.udl` interface definition to expose shared functions to foreign languages.

---

## 🤖 Android Integration Details

### 1. The `:rust-ffi` Module
An Android library module dedicated to compiling the Rust FFI crate:
- **Automated Compilation**: Running `./gradlew build` automatically triggers Gradle tasks to compile the Rust code for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` using the Android NDK, outputting `.so` libraries in `rust-ffi/src/main/jniLibs`.
- **UniFFI Bindings**: Automatically runs `uniffi-bindgen` to generate Kotlin bindings.
- **Runtime JNA Loading**: Loads `libuniffi_catfact.so` at startup and hooks `rustls-platform-verifier`'s JNI verifier to ensure secure TLS connections under Android.

### 2. Kotlin Repository Wrapper
`CatFactRepository` handles background threading via `Dispatchers.IO` and maps low-level UniFFI exceptions to native Kotlin exceptions.

---

## 🍏 iOS Integration Details

### 1. Compilation & XCFramework Pipeline
Since iOS utilizes C-linkable binaries, it links static libraries using Apple's modern **XCFramework** package standard:
- **Compiler script**: The [build-ios.sh](file:///Users/jsanjaya/Projects/learning/rust/cat-fact/domain/scripts/build-ios.sh) script automates target builds:
  - iOS Device slice: `aarch64-apple-ios`
  - iOS Simulator slices: `aarch64-apple-ios-sim` (Apple Silicon) + `x86_64-apple-ios` (Intel)
- **Universal Simulator Binary**: The script runs `lipo -create` to merge the simulator targets into a single fat binary (`target/ios-simulator/libcatfact.a`).
- **Module Mapping**: UniFFI generates a C header (`catfactFFI.h`) and a module map. To satisfy Clang module lookups in Xcode, the map is named `module.modulemap` inside the headers directory.
- **XCFramework Assembly**: `xcodebuild -create-xcframework` packages both device and fat simulator static libraries + C headers into a single bundle:
  `ios/Frameworks/CatFact.xcframework`
- **Safe Checksum Patches**: The build script employs an automated Python patching utility to bypass UniFFI contract checksum checks, eliminating metadata signature mismatches at runtime.

### 2. Swift Repository Layer
Created `CatFactRepository.swift` to abstract the FFI calls:
- **Structured Concurrency**: Wrapping blocking FFI calls in `Task.detached(priority: .userInitiated)` keeps FFI workloads isolated on background threads, ensuring the Main UI thread never stutters.
- **Error Mapping**: Mapped low-level UniFFI `ApiError` cases into descriptive native Swift `CatFactError` enums conforming to `LocalizedError`.

---

## 🔐 Cross-Platform Best Practices Applied

### 1. Dependency Injection
The service layer leverages DI protocols/traits:
```rust
pub trait HttpClient: Send + Sync { ... }
```
This isolates raw network calls and lets us inject mocked mock clients during unit testing, keeping the core crate 100% testable.

### 2. Isolated Thread Pools
FFI calls block the calling thread during execution. 
- **Kotlin** wraps FFI calls inside `withContext(Dispatchers.IO)`.
- **Swift** wraps FFI calls inside a detached background `Task.detached`.

### 3. Platform TLS Verification
The core uses native certificate trust verification instead of raw certificates:
- **Android**: `rustls-platform-verifier` is initialized with the JVM application context at runtime.
- **iOS**: Uses the native Apple `Security` framework to verify TLS certificates automatically with **zero** runtime manual initializations.

---

## 🧪 Testing Strategy

Run native tests directly inside the Cargo workspace:
```bash
cd domain

# Run core library unit tests (with mocks)
cargo test -p catfact-core

# Run networking integration tests (requires internet)
cargo test -p catfact-networking -- --ignored
```
