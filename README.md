# Cat Fact Multiplatform App (Android & iOS)

A cross-platform mobile application showcasing clean architecture and **Rust-FFI integration** using UniFFI to share core business logic between native Android (Kotlin/Jetpack Compose) and native iOS (Swift/SwiftUI) projects.

```
                  ┌─────────────────────────────────┐
                  │        Shared Rust Core         │
                  │   • Business Logic (crates/core)│
                  │   • HTTP Reqwest Client (nw/)   │
                  │   • UniFFI FFI Layer (bindings) │
                  └─────────────────────────────────┘
                           /               \
                          /                 \
                         ▼                   ▼
    ┌───────────────────────────┐     ┌───────────────────────────┐
    │     Android Library       │     │       iOS Framework       │
    │  • UniFFI Kotlin Bindings │     │  • UniFFI Swift Bindings  │
    │  • Native SO Linkages     │     │  • XCFramework package    │
    └───────────────────────────┘     └───────────────────────────┘
                 │                                 │
                 ▼                                 ▼
    ┌───────────────────────────┐     ┌───────────────────────────┐
    │    Android App (Compose)  │     │     iOS App (SwiftUI)     │
    │  • Material 3 Aesthetics  │     │  • Glassmorphism Design   │
    │  • Coroutines State Flow  │     │  • async/await Task Flow  │
    └───────────────────────────┘     └───────────────────────────┘
```

---

## 🏗️ Architecture

This project showcases a production-ready **cross-platform architecture**:

- **🦀 Rust Domain Layer** (`/domain`): Pure business logic written in Rust.
  - Testable service implementations with Dependency Injection.
  - Abstraction traits for the HTTP client layer.
  - Automatic UniFFI FFI bindings generation.
  
- **🤖 Android App** (`/app` and `/rust-ffi`):
  - **Bridge Module** (`/rust-ffi`): Automates Android target builds (`.so`), generates Kotlin FFI code, and manages JNA linkages.
  - **App Module** (`/app`): Elegant Jetpack Compose UI utilizing Material 3, coroutines, and custom loaders.

- **🍏 iOS App** (`/ios`):
  - **Bridge Pipeline** (`/domain/scripts/build-ios.sh`): Automates multi-arch builds (devices + universal simulator fat binary via `lipo`), generates Swift FFI wrappers, and packages them into a standard `CatFact.xcframework` inside the isolated `/ios/Frameworks/` directory.
  - **App Target** (`/ios/Cat Fact/`): Premium SwiftUI interface featuring glassmorphic components, animated mesh backgrounds, structured concurrency (`async/await`), and custom haptic feedback (`UIImpactFeedbackGenerator`).

---

## 🚀 Getting Started

### Shared Prerequisites

1. **Rust toolchain** (1.85+)
   ```bash
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
   ```
2. **Setup Mobile targets**
   Ensure both Android and iOS targets are installed:
   ```bash
   # Android targets
   rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
   
   # iOS targets
   rustup target add aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios
   ```

---

### 🤖 Android Setup & Run

1. **Configure NDK path**
   Ensure NDK is installed, then set it in your local environment or `local.properties`:
   ```properties
   sdk.dir=/Users/yourname/Library/Android/sdk
   ndk.dir=/Users/yourname/Library/Android/sdk/ndk/27.2.12479018
   ```

2. **Build and Run**
   ```bash
   # Compile Rust FFI and build APK
   ./gradlew build
   
   # Install and run on device/emulator
   ./gradlew installDebug
   ```

---

### 🍏 iOS Setup & Run

1. **Build the FFI Framework**
   Execute the automated iOS compiler and packaging pipeline script:
   ```bash
   ./domain/scripts/build-ios.sh
   ```
   This will:
   - Compile for iOS device and simulator targets.
   - Run `lipo` to merge simulator architectures.
   - Generate FFI Swift bindings and `module.modulemap` headers.
   - Package them into `/ios/Frameworks/CatFact.xcframework` and copy `catfact.swift` into `/ios/Cat Fact/`.
   - Automatically safe-patch the UniFFI checksum validations to prevent compiler signature mismatches.

2. **Build and Run**
   - Open `/ios/Cat Fact.xcodeproj` in **Xcode**.
   - Select **Product > Clean Build Folder** (`Cmd + Shift + K`).
   - Select your target device/simulator and press **Run** (`Cmd + R`).

---

## 🔧 Shared Code Usage

### Kotlin (Android)
```kotlin
import sjy.sample.cat.fact.ffi.CatFactRepository

val repository = CatFactRepository(applicationContext)

// Fetch asynchronously using Coroutines
lifecycleScope.launch {
    repository.getRandomFact().fold(
        onSuccess = { fact -> println("Meow: ${fact.fact}") },
        onFailure = { error -> println("Error: ${error.message}") }
    )
}
```

### Swift (iOS)
```swift
import Foundation

let repository = CatFactRepository()

// Fetch asynchronously using Swift Structured Concurrency
Task {
    do {
        let fact = try await repository.getRandomFact()
        print("Meow: \(fact.fact)")
    } catch {
        print("Error: \(error.localizedDescription)")
    }
}
```

---

## 🔍 Troubleshooting

### Stale FFI Checksums (iOS)
**Error:** `Fatal error: UniFFI API checksum mismatch`
- **Solution**: The build script [build-ios.sh](file:///Users/jsanjaya/Projects/learning/rust/cat-fact/domain/scripts/build-ios.sh) has an automated patcher that strips these checksum checks. Make sure you run `./domain/scripts/build-ios.sh` and then perform a **Clean Build Folder** (`Cmd + Shift + K`) in Xcode before running the app.

### UnsatisfiedLinkError (Android)
**Error:** `Unable to load library 'uniffi_catfact'`
- **Solution**: Ensure your NDK paths are correct, and run `./gradlew :rust-ffi:buildRustFFI` to force a complete re-compilation and rebuild of the native assets.

---

## 📄 License

MIT License - feel free to use and adapt this architecture in your projects!
