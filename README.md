# Cat Fact Multiplatform App (Android, iOS & Web React)

A cross-platform application showcasing clean architecture and **Rust-FFI integration** using WebAssembly (WASM) and UniFFI to share core business logic between native Android (Kotlin/Jetpack Compose), native iOS (Swift/SwiftUI), and Web React projects.

```
                           ┌───────────────────────────────────┐
                           │         Shared Rust Core          │
                           │   • Core business logic (core)    │
                           │   • Reqwest HTTP Client (nw/)     │
                           └───────────────────────────────────┘
                                  /            |            \
                                 /             |             \
                                ▼              ▼              ▼
                    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
                    │ Android Lib  │    │ iOS FFI Lib  │    │ WASM Wrapper │
                    │ • SO Linkage │    │ • Static Lib │    │ • ES Modules │
                    │ • Kotlin Bind│    │ • Swift Bind │    │ • Browser JS │
                    └──────────────┘    └──────────────┘    └──────────────┘
                           │                   │                   │
                           ▼                   ▼                   ▼
                    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
                    │ Android App  │    │   iOS App    │    │  Web App     │
                    │ • Jetpack    │    │ • SwiftUI    │    │ • React      │
                    │   Compose    │    │ • Glassmorphic│    │ • Glassmorphic│
                    └──────────────┘    └──────────────┘    └──────────────┘
```

---

## 🏗️ Architecture

This project showcases a production-ready **cross-platform architecture**:

- **🦀 Rust Domain Layer** (`/domain`): Pure business logic written in Rust.
  - Testable service implementations with Dependency Injection.
  - Abstraction traits for the HTTP client layer.
  - Automatic UniFFI FFI bindings generation for mobile.
  - Target-conditional compiled `reqwest` clients optimized for browser single-threaded, non-`Send` environments.
  
- **🌐 Web React App** (`/domain/bindings/catfact-wasm`):
  - **WASM Binding Crate** (`/domain/bindings/catfact-wasm`): Exposes simple JS-friendly, Promise-based bindings.
  - **Build Automation** (`/domain/scripts/build-wasm.sh`): Downloads `wasm-pack` and generates a clean ES module.

- **🤖 Android App** (`/app` and `/rust-ffi`):
  - **Bridge Module** (`/rust-ffi`): Automates Android target builds (`.so`), generates Kotlin FFI code, and manages JNA linkages.
  - **App Module** (`/app`): Elegant Jetpack Compose UI utilizing Material 3, coroutines, and custom loaders.

- **🍏 iOS App** (`/ios`):
  - **Bridge Pipeline** (`/domain/scripts/build-ios.sh`): Automates multi-arch builds (devices + simulator fat binary), generates Swift FFI wrappers, and packages them into a standard `CatFact.xcframework` inside the `/ios/Frameworks/` directory.
  - **App Target** (`/ios/Cat Fact/`): Premium SwiftUI interface featuring glassmorphic components, animated mesh backgrounds, structured concurrency (`async/await`), and custom haptic feedback (`UIImpactFeedbackGenerator`).

---

## 🚀 Getting Started

### Shared Prerequisites

1. **Rust toolchain** (1.96+)
   ```bash
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
   ```
2. **Setup Mobile & Web targets**
   Ensure all target architectures are installed:
   ```bash
   # Android targets
   rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
   
   # iOS targets
   rustup target add aarch64-apple-ios aarch64-apple-ios-sim x86_64-apple-ios

   # WebAssembly target
   rustup target add wasm32-unknown-unknown
   ```

---

### 🌐 Web (WASM & React) Setup & Run

1. **Build the WASM Package**
   Run the automated build script inside the `domain` directory:
   ```bash
   ./domain/scripts/build-wasm.sh
   ```
   This compiles the WASM binary, installs `wasm-pack` if missing, disables `wasm-opt` (bypassing bulk-memory validation errors), and packages the ES module inside `domain/bindings/catfact-wasm/pkg`.

2. **Integrate into React**
   Link the generated package inside your React `package.json` file:
   ```json
   "dependencies": {
     "catfact-wasm": "file:../path/to/cat-fact/domain/bindings/catfact-wasm/pkg"
   }
   ```
   Follow the details and styling components in the [WASM README](file:///Users/jsanjaya/Projects/learning/rust/cat-fact/domain/bindings/catfact-wasm/README.md) to integrate the premium React component.

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
   This will compile for iOS device and simulator targets, lipo/merge simulator slices, package them into `CatFact.xcframework`, copy Swift bindings, and patch UniFFI checksum validations to prevent signature mismatches.

2. **Build and Run**
   - Open `/ios/Cat Fact.xcodeproj` in **Xcode**.
   - Select **Product > Clean Build Folder** (`Cmd + Shift + K`).
   - Select your target device/simulator and press **Run** (`Cmd + R`).

---

## 🔧 Shared Code Usage

### React / JS (Web)
```typescript
import initWasm, { CatFactRepository } from 'catfact-wasm';

async function run() {
  await initWasm();
  const repository = new CatFactRepository();
  const result = await repository.get_random_fact();
  console.log("Fact:", result.fact);
}
```

### Kotlin (Android)
```kotlin
import sjy.sample.cat.fact.ffi.CatFactRepository

val repository = CatFactRepository(applicationContext)

// Fetch asynchronously using Coroutines
lifecycleScope.launch {
    runCatching { repository.getRandomFact().fact }
        .onSuccess { fact -> println("Meow: $fact") }
        .onFailure { error -> println("Error: ${error.message}") }
}
```

### Swift (iOS)
```swift
import Foundation

let repository = try! CatFactRepository()

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
- **Solution**: Make sure you run `./domain/scripts/build-ios.sh` and perform a **Clean Build Folder** (`Cmd + Shift + K`) in Xcode before running the app.

### UnsatisfiedLinkError (Android)
- **Solution**: Ensure your NDK paths are correct, and run `./gradlew :rust-ffi:buildRustFFI` to force a complete re-compilation.

---

## 📄 License

MIT License - feel free to use and adapt this architecture in your projects!
