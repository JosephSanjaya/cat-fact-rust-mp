# Rust-Android Integration Guide

This guide explains how the Rust domain layer is integrated into the Android app using best practices from the rust-expert skill.

## 🏗️ Architecture Overview

The integration follows a **three-layer architecture**:

```
┌─────────────────────────────────────────────────────────┐
│              Android App (Kotlin/Compose)               │
│  • UI Layer (Jetpack Compose)                          │
│  • ViewModel/State Management                          │
│  • Kotlin Coroutines                                   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│           Rust FFI Module (:rust-ffi)                   │
│  • CatFactRepository (Kotlin wrapper)                   │
│  • UniFFI-generated Kotlin bindings                     │
│  • JNA native library loading                          │
│  • Automated Gradle build tasks                        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│         Rust Domain Layer (domain/)                     │
│  • Core business logic (catfact-core)                   │
│  • HTTP client (catfact-networking)                     │
│  • FFI bindings (catfact-ffi)                          │
│  • UniFFI interface definitions                        │
└─────────────────────────────────────────────────────────┘
```

## 🔧 Key Components

### 1. Rust Domain Layer (`domain/`)

**Structure:**
```
domain/
├── crates/
│   ├── core/              # Pure business logic
│   │   ├── models.rs      # CatFact, errors
│   │   └── service.rs     # CatFactService with DI
│   └── networking/        # HTTP implementations
│       └── lib.rs         # ReqwestHttpClient
├── bindings/
│   └── catfact-ffi/       # UniFFI bindings
│       ├── src/
│       │   ├── lib.rs     # FFI wrapper
│       │   └── catfact.udl # UniFFI definition
│       └── build.rs       # Build script
└── Cargo.toml             # Workspace config
```

**Key Features:**
- **Dependency Injection**: `HttpClient` trait for testability
- **Composition Over Inheritance**: Services compose behavior
- **Production Optimizations**: LTO, strip, opt-level=3
- **Thread Safety**: Arc for shared ownership
- **Panic Safety**: `panic = "unwind"` for FFI safety

### 2. Rust FFI Module (`:rust-ffi`)

**Purpose:** Bridge between Rust and Android

**Responsibilities:**
1. **Automated Rust Compilation**
   - Installs Rust targets for Android ABIs
   - Compiles Rust library for arm64-v8a, armeabi-v7a, x86, x86_64
   - Copies `.so` files to `jniLibs/`

2. **Kotlin Binding Generation**
   - Installs `uniffi-bindgen` CLI
   - Generates Kotlin bindings from `.udl` file
   - Places bindings in `build/generated/uniffi/kotlin/`

3. **Kotlin Wrapper Layer**
   - `CatFactRepository`: Coroutine-friendly API
   - Error conversion: Rust errors → Kotlin exceptions
   - Thread management: IO dispatcher for FFI calls

**Gradle Tasks:**
```bash
# Build everything (runs automatically on preBuild)
./gradlew :rust-ffi:buildRustFFI

# Individual tasks
./gradlew :rust-ffi:checkRustToolchain
./gradlew :rust-ffi:installRustTargets
./gradlew :rust-ffi:buildRustLibrary
./gradlew :rust-ffi:copyRustLibraries
./gradlew :rust-ffi:generateKotlinBindings
./gradlew :rust-ffi:cleanRust
```

### 3. Android App (`:app`)

**Integration:**
```kotlin
// Add dependency in app/build.gradle.kts
dependencies {
    implementation(project(":rust-ffi"))
}

// Use in Kotlin code
val repository = CatFactRepository()

lifecycleScope.launch {
    val result = repository.getRandomFact()
    result.fold(
        onSuccess = { fact -> /* Handle success */ },
        onFailure = { error -> /* Handle error */ }
    )
}
```

## 🚀 Build Process

### Automated Build Flow

When you run `./gradlew build`, the following happens automatically:

1. **Pre-Build Hook** (`preBuild` task depends on `buildRustFFI`)
   
2. **Check Rust Toolchain**
   - Verifies `cargo` is installed
   - Fails fast if Rust is not available

3. **Install Rust Targets**
   - Installs Android targets if not present:
     - `aarch64-linux-android` (arm64-v8a)
     - `armv7-linux-androideabi` (armeabi-v7a)
     - `i686-linux-android` (x86)
     - `x86_64-linux-android` (x86_64)

4. **Build Rust Library**
   - Reads `ANDROID_NDK_HOME` from environment or `local.properties`
   - Compiles Rust library for all targets in release mode
   - Output: `domain/target/{target}/release/libcatfact.so`

5. **Copy Native Libraries**
   - Copies `.so` files to `rust-ffi/src/main/jniLibs/{abi}/`
   - Android build system automatically includes these in APK

6. **Generate Kotlin Bindings**
   - Installs `uniffi-bindgen` if not present
   - Generates Kotlin bindings from `catfact.udl`
   - Output: `rust-ffi/build/generated/uniffi/kotlin/uniffi/catfact/`

7. **Compile Android App**
   - Kotlin compiler includes generated bindings
   - Native libraries are packaged into APK

### Manual Build (Optional)

You can also build the Rust library manually:

```bash
cd domain
./scripts/build-android.sh
```

This is useful for:
- Debugging Rust compilation issues
- Testing Rust changes without full Android build
- CI/CD pipelines

## 🔐 Best Practices Applied

### 1. **Dependency Injection**

The Rust layer uses trait-based DI:

```rust
pub trait HttpClient: Send + Sync {
    fn get<'a>(&'a self, url: &'a str, headers: Vec<(&'a str, &'a str)>) 
        -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>>;
}

pub struct CatFactService<C: HttpClient> {
    client: C,
    // ...
}
```

**Benefits:**
- Easy to mock for testing
- Platform-specific implementations (URLSession for iOS, OkHttp for Android)
- Loose coupling between layers

### 2. **Composition Over Inheritance**

Services compose behavior instead of inheriting:

```rust
pub struct CatFactService<C: HttpClient> {
    client: C,  // Composed, not inherited
    base_url: String,
    csrf_token: Option<String>,
}
```

### 3. **Thread Safety**

FFI layer uses `Arc` for thread-safe sharing:

```rust
pub struct CatFactApi {
    runtime: Arc<tokio::runtime::Runtime>,
    service: Arc<CatFactService<ReqwestHttpClient>>,
}
```

Kotlin wrapper uses IO dispatcher:

```kotlin
suspend fun getRandomFact(): Result<CatFact> = withContext(Dispatchers.IO) {
    // FFI call runs on IO thread pool
}
```

### 4. **Error Handling**

**Rust side:**
```rust
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ApiError {
    #[error("Network error: {message}")]
    NetworkError { message: String },
    // ...
}
```

**Kotlin side:**
```kotlin
sealed class CatFactException(message: String) : Exception(message) {
    data class NetworkError(override val message: String) : CatFactException(message)
    // ...
}
```

### 5. **Memory Management**

- **Rust**: Ownership system ensures memory safety
- **UniFFI**: Generates lifecycle management code
- **Kotlin**: `AutoCloseable` for resource cleanup (if needed)
- **JNA**: Handles native library loading and unloading

### 6. **Performance Optimizations**

**Rust release profile:**
```toml
[profile.release]
opt-level = 3          # Maximum optimization
lto = true             # Link-time optimization
codegen-units = 1      # Single codegen unit
strip = true           # Remove debug symbols (~40% size reduction)
panic = "unwind"       # Required for FFI safety
```

**HTTP client:**
- Connection pooling (10 connections per host)
- 90-second idle timeout
- Reuses TCP connections

**Tokio runtime:**
- 2 worker threads (sufficient for I/O-bound work)
- Multi-threaded for better mobile performance

### 7. **ProGuard/R8 Configuration**

```proguard
# Keep UniFFI generated classes
-keep class uniffi.catfact.** { *; }

# Keep JNA classes
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure {
    *;
}
```

## 🧪 Testing Strategy

### Rust Tests

```bash
cd domain

# Unit tests with mocked HTTP client
cargo test -p catfact-core

# Integration tests (requires network)
cargo test -p catfact-networking -- --ignored

# FFI tests
cargo test -p catfact-ffi
```

### Android Tests

```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Instrumented tests
./gradlew :app:connectedDebugAndroidTest
```

## 📦 Binary Size

Typical sizes (release, stripped):

| ABI          | Size    |
|--------------|---------|
| arm64-v8a    | ~2.5 MB |
| armeabi-v7a  | ~2.3 MB |
| x86          | ~2.8 MB |
| x86_64       | ~2.7 MB |

**Total APK overhead:** ~10 MB for all ABIs

**Optimization tips:**
- Use App Bundles (AAB) for per-device ABI delivery
- Remove unused ABIs for specific builds
- Enable R8 full mode for additional shrinking

## 🔍 Debugging

### Rust Logs

Rust uses platform-specific logging:

**Android (logcat):**
```bash
adb logcat | grep CatFactAPI
```

**Rust code:**
```rust
log::info!("Fetching cat fact from {}", url);
log::error!("Network error: {}", error);
```

### Kotlin Logs

```kotlin
Log.d("CatFact", "Fetching fact...")
```

### Common Issues

**1. NDK Not Found**
```
Error: ANDROID_NDK_HOME not set
```
**Solution:** Set in `local.properties`:
```properties
ndk.dir=/path/to/android/sdk/ndk/27.2.12479018
```

**2. Rust Target Not Installed**
```
Error: can't find crate for 'std'
```
**Solution:**
```bash
rustup target add aarch64-linux-android
```

**3. Native Library Not Loaded**
```
UnsatisfiedLinkError: Unable to load library 'catfact'
```
**Solution:**
```bash
./gradlew :rust-ffi:buildRustLibrary
./gradlew :rust-ffi:copyRustLibraries
```

## 🚢 CI/CD Integration

### GitHub Actions Example

```yaml
name: Android Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install Rust
        uses: actions-rs/toolchain@v1
        with:
          toolchain: stable
          profile: minimal
      
      - name: Install Android targets
        run: |
          rustup target add aarch64-linux-android
          rustup target add armv7-linux-androideabi
          rustup target add i686-linux-android
          rustup target add x86_64-linux-android
      
      - name: Set up Android NDK
        uses: nttld/setup-ndk@v1
        with:
          ndk-version: r27
      
      - name: Build with Gradle
        run: ./gradlew build
        env:
          ANDROID_NDK_HOME: ${{ steps.setup-ndk.outputs.ndk-path }}
```

## 📚 Further Reading

- [Rust Domain Layer Documentation](domain/README.md)
- [Architecture Documentation](domain/ARCHITECTURE.md)
- [UniFFI User Guide](https://mozilla.github.io/uniffi-rs/)
- [Rust FFI Best Practices](https://doc.rust-lang.org/nomicon/ffi.html)
- [Android NDK Guide](https://developer.android.com/ndk/guides)

## 🎯 Next Steps

1. **Add More Endpoints**: Extend `CatFactService` with new methods
2. **Implement Caching**: Add local storage layer in Rust
3. **Add iOS Support**: Use the same Rust code for iOS via XCFramework
4. **Optimize Binary Size**: Use `wasm-opt` techniques for mobile
5. **Add Metrics**: Instrument Rust code with metrics collection
6. **Implement Retry Logic**: Add exponential backoff in HTTP client
7. **Add Certificate Pinning**: Enhance security with cert pinning

## 🤝 Contributing

When adding new Rust functionality:

1. Add domain logic to `domain/crates/core/`
2. Add HTTP implementations to `domain/crates/networking/`
3. Expose via FFI in `domain/bindings/catfact-ffi/src/lib.rs`
4. Update UniFFI definition in `catfact.udl`
5. Update Kotlin wrapper in `rust-ffi/src/main/kotlin/`
6. Test both Rust and Android layers

## 📄 License

MIT License - feel free to use in your projects!
