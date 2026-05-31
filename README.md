# Cat Fact Android App

An Android application demonstrating Rust-Android integration using UniFFI for fetching cat facts.

## 🏗️ Architecture

This project showcases a **multiplatform architecture** with:

- **Rust Domain Layer** (`/domain`): Core business logic written in Rust
  - Pure domain models and service layer
  - HTTP client abstraction for testability
  - UniFFI bindings for Android/iOS interop
  
- **Android App** (`/app`): Jetpack Compose UI
  - Modern Android UI with Material 3
  - Kotlin Coroutines for async operations
  
- **Rust FFI Module** (`/rust-ffi`): Bridge between Rust and Android
  - Automated Rust compilation for Android targets
  - Kotlin binding generation via UniFFI
  - JNA-based native library loading

## 🚀 Getting Started

### Prerequisites

1. **Android Studio** (latest stable version)
2. **Rust toolchain** (1.85+)
   ```bash
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
   ```

3. **Android NDK** (27.x or later)
   - Install via Android Studio SDK Manager
   - Or set `ANDROID_NDK_HOME` environment variable

4. **Rust Android targets**
   ```bash
   rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
   ```

5. **uniffi-bindgen CLI** (optional, auto-installed by Gradle)
   ```bash
   cargo install uniffi-bindgen --version 0.31
   ```

### Building the Project

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd cat-fact
   ```

2. **Configure NDK path**
   
   Create or edit `local.properties`:
   ```properties
   sdk.dir=/path/to/android/sdk
   ndk.dir=/path/to/android/sdk/ndk/27.2.12479018
   ```
   
   Or set environment variable:
   ```bash
   export ANDROID_NDK_HOME=/path/to/android/sdk/ndk/27.2.12479018
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```
   
   This will automatically:
   - Install required Rust targets
   - Compile Rust library for all Android ABIs
   - Generate Kotlin bindings via UniFFI
   - Copy native libraries to `jniLibs`

4. **Run the app**
   ```bash
   ./gradlew installDebug
   ```
   
   Or use Android Studio's Run button.

## 🔧 Development Workflow

### Gradle Tasks

The `:rust-ffi` module provides several Gradle tasks:

```bash
# Build Rust library and generate bindings
./gradlew :rust-ffi:buildRustFFI

# Only build Rust library
./gradlew :rust-ffi:buildRustLibrary

# Only generate Kotlin bindings
./gradlew :rust-ffi:generateKotlinBindings

# Clean Rust artifacts
./gradlew :rust-ffi:cleanRust

# Check Rust toolchain
./gradlew :rust-ffi:checkRustToolchain
```

### Manual Rust Build (Optional)

You can also build the Rust library manually:

```bash
cd domain
./scripts/build-android.sh
```

### Using the Rust FFI in Kotlin

```kotlin
import sjy.sample.cat.fact.ffi.CatFactRepository

// Create repository
val repository = CatFactRepository()

// Fetch a cat fact (suspend function)
lifecycleScope.launch {
    val result = repository.getRandomFact()
    result.fold(
        onSuccess = { fact -> 
            println("Fact: ${fact.fact}")
            println("Length: ${fact.length}")
        },
        onFailure = { error -> 
            println("Error: ${error.message}")
        }
    )
}

// Or use blocking call (not recommended on main thread)
val result = repository.getRandomFactBlocking()
```

## 📦 Project Structure

```
cat-fact/
├── app/                          # Android application module
│   ├── src/main/
│   │   ├── java/                 # Kotlin source code
│   │   └── res/                  # Android resources
│   └── build.gradle.kts
│
├── rust-ffi/                     # Rust FFI wrapper module
│   ├── src/main/
│   │   ├── kotlin/               # Kotlin wrapper classes
│   │   └── jniLibs/              # Native libraries (generated)
│   └── build.gradle.kts          # Automated Rust build tasks
│
├── domain/                       # Rust domain layer
│   ├── crates/
│   │   ├── core/                 # Pure business logic
│   │   └── networking/           # HTTP implementations
│   ├── bindings/
│   │   └── catfact-ffi/          # UniFFI bindings
│   ├── scripts/
│   │   ├── build-android.sh      # Manual Android build script
│   │   └── build-ios.sh          # iOS build script
│   └── Cargo.toml                # Rust workspace configuration
│
└── sjy-build-logic/              # Gradle convention plugins
```

## 🧪 Testing

### Rust Tests

```bash
cd domain

# Run all tests
cargo test

# Run with output
cargo test -- --nocapture

# Run only core tests
cargo test -p catfact-core
```

### Android Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## 🔍 Troubleshooting

### NDK Not Found

**Error:** `ANDROID_NDK_HOME not set`

**Solution:** 
1. Install NDK via Android Studio SDK Manager
2. Set in `local.properties`:
   ```properties
   ndk.dir=/path/to/android/sdk/ndk/27.2.12479018
   ```
3. Or set environment variable:
   ```bash
   export ANDROID_NDK_HOME=/path/to/android/sdk/ndk/27.2.12479018
   ```

### Rust Target Not Installed

**Error:** `error: can't find crate for 'std'`

**Solution:**
```bash
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
```

### uniffi-bindgen Not Found

**Error:** `uniffi-bindgen: command not found`

**Solution:** Gradle will auto-install it, or install manually:
```bash
cargo install uniffi-bindgen --version 0.31
```

### Native Library Not Loaded

**Error:** `java.lang.UnsatisfiedLinkError: Unable to load library 'catfact'`

**Solution:**
1. Ensure Rust library was built: `./gradlew :rust-ffi:buildRustLibrary`
2. Check `rust-ffi/src/main/jniLibs/` contains `.so` files
3. Clean and rebuild: `./gradlew clean build`

### Gradle Sync Issues

**Solution:**
```bash
./gradlew --stop
./gradlew clean
./gradlew build --refresh-dependencies
```

## 🎯 Best Practices

### Rust-Android Integration

1. **Automated Builds**: The Gradle build automatically compiles Rust and generates bindings
2. **Thread Safety**: Rust FFI calls run on IO dispatcher to avoid blocking main thread
3. **Error Handling**: Rust errors are converted to Kotlin sealed exceptions
4. **Memory Management**: UniFFI handles memory lifecycle automatically
5. **ProGuard**: Keep rules are configured for JNA and UniFFI classes

### Performance Considerations

- Rust library uses **LTO** (Link-Time Optimization) for maximum performance
- **Connection pooling** in Reqwest reduces HTTP latency
- **Multi-threaded Tokio runtime** (2 workers) for async operations
- **Stripped binaries** reduce APK size by ~40%

## 📚 Resources

- [Rust Domain Layer Documentation](domain/README.md)
- [Architecture Documentation](domain/ARCHITECTURE.md)
- [UniFFI Documentation](https://mozilla.github.io/uniffi-rs/)
- [Cat Fact API](https://catfact.ninja)

## 📄 License

MIT License - feel free to use in your projects!
