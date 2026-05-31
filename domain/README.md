# Cat Fact API - Multiplatform Rust Library

A production-ready, multiplatform Rust library for fetching cat facts from the [Cat Fact API](https://catfact.ninja). Designed with best practices including dependency injection, composition over inheritance, and clean architecture.

## 🎯 Features

- **Pure Business Logic**: Core domain models with zero I/O dependencies
- **Dependency Injection**: HTTP client abstraction for easy testing and platform-specific implementations
- **Multiplatform Support**: Android, iOS, Web (WASM), and native Rust
- **Type-Safe FFI**: Automatic bindings generation via UniFFI for Kotlin, Swift, and Python
- **Production-Ready**: Optimized release profile with LTO, connection pooling, and proper error handling
- **Testable**: Mock-friendly architecture with comprehensive unit tests

## 📦 Architecture

```
new-project/
├── crates/
│   ├── core/              # Pure business logic (no I/O)
│   │   ├── models.rs      # Domain models (CatFact, errors)
│   │   └── service.rs     # Business logic with DI
│   └── networking/        # HTTP client implementations
│       └── lib.rs         # Reqwest-based client
├── bindings/
│   └── catfact-ffi/       # UniFFI bindings for mobile/FFI
│       ├── src/
│       │   ├── lib.rs     # FFI wrapper
│       │   └── catfact.udl # UniFFI interface definition
│       └── build.rs       # Build script
└── src/
    └── main.rs            # Example CLI application
```

## 🏗️ Design Principles

### 1. Dependency Injection
The `HttpClient` trait allows injecting different implementations:
- **Testing**: Mock clients for unit tests
- **Native**: Reqwest for CLI/server applications
- **iOS**: URLSession delegation (future)
- **Android**: OkHttp delegation (future)
- **Web**: Fetch API via wasm-bindgen (future)

### 2. Composition Over Inheritance
- Services compose behavior through trait implementations
- No deep inheritance hierarchies
- Easy to extend with new capabilities

### 3. Separation of Concerns
- **Core**: Pure business logic, no I/O
- **Networking**: Concrete HTTP implementations
- **FFI**: Platform bindings layer

## 🚀 Quick Start

### Prerequisites

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install required targets (for mobile)
rustup target add aarch64-apple-ios aarch64-apple-ios-sim
rustup target add aarch64-linux-android armv7-linux-androideabi
```

### Run the Example CLI

```bash
# Build and run
cargo run

# Or build optimized release
cargo build --release
./target/release/catfact-cli
```

### Run Tests

```bash
# Run all tests
cargo test

# Run tests with output
cargo test -- --nocapture

# Run only core tests
cargo test -p catfact-core
```

## 📱 Platform Integration

### Android

1. **Build the library:**
```bash
# Install cargo-ndk
cargo install cargo-ndk

# Build for Android targets
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o ./android/app/src/main/jniLibs build --release -p catfact-ffi
```

2. **Generate Kotlin bindings:**
```bash
cargo run --bin uniffi-bindgen generate bindings/catfact-ffi/src/catfact.udl --language kotlin --out-dir ./android/app/src/main/kotlin
```

3. **Use in Kotlin:**
```kotlin
import sjy.sample.cat.fact.ffi.CatFactRepository

// Create Repository client (initializes NDK & platform trust verifiers)
val repository = CatFactRepository(context)

// Fetch a fact asynchronously using suspend functions
try {
    val fact = repository.getRandomFact()
    println("Fact: ${fact.fact}")
} catch (e: Exception) {
    println("Failed to fetch fact: ${e.message}")
}
```

### iOS

1. **Build XCFramework and bindings automatically:**
```bash
# Execute the automated iOS build pipeline script
./scripts/build-ios.sh
```
This automatically compiles for all architectures, merges simulator targets using `lipo`, outputs `CatFact.xcframework` under `../ios/Frameworks/`, and generates `catfact.swift` in `../ios/Cat Fact/` with automatic UniFFI checksum bypass patches.

2. **Use in Swift (SwiftUI Repository pattern):**
```swift
import Foundation

// Use the isolated Swift repository wrapper with structured concurrency
let repository = CatFactRepository()

Task {
    do {
        let fact = try await repository.getRandomFact()
        print("Fact: \(fact.fact) (\(fact.length) characters)")
    } catch {
        print("Error: \(error.localizedDescription)")
    }
}
```

### Web (WASM)

```bash
# Install wasm-pack
cargo install wasm-pack

# Build for web (requires wasm-bindgen integration)
wasm-pack build --target web
```

## 🧪 Testing Strategy

### Unit Tests
```rust
// Mock HTTP client for testing
struct MockHttpClient {
    response: Arc<str>,
}

impl HttpClient for MockHttpClient {
    fn get<'a>(&'a self, _url: &'a str, _headers: Vec<(&'a str, &'a str)>) 
        -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>> 
    {
        let response = self.response.clone();
        Box::pin(async move { Ok(response.to_string()) })
    }
}

#[tokio::test]
async fn test_service() {
    let client = MockHttpClient { 
        response: Arc::from(r#"{"fact":"Test","length":4}"#) 
    };
    let service = CatFactService::new(client);
    let result = service.get_random_fact().await;
    assert!(result.is_ok());
}
```

## 🔧 Configuration

### Custom Base URL and CSRF Token

```rust
use catfact_core::CatFactService;
use catfact_networking::ReqwestHttpClient;

let client = ReqwestHttpClient::new()?;
let service = CatFactService::with_config(
    client,
    "https://custom-api.example.com".to_string(),
    Some("your-csrf-token".to_string()),
);
```

### FFI Configuration

```kotlin
// Android/Kotlin
val config = ApiConfig(
    baseUrl = "https://custom-api.example.com",
    csrfToken = "your-token"
)
val repository = uniffi.catfact.CatFactRepository.withConfig(config)
```

```swift
// iOS/Swift
let config = ApiConfig(
    baseUrl: "https://custom-api.example.com",
    csrfToken: "your-token"
)
let repository = try CatFactRepository.withConfig(config: config)
```

## 🎯 Performance Optimizations

- **LTO (Link-Time Optimization)**: Whole-program optimization
- **Connection Pooling**: Reuses HTTP connections (10 per host)
- **Stripped Binaries**: ~40% size reduction in release builds
- **Async Runtime**: Multi-threaded Tokio runtime (2 workers for FFI)
- **Timeout Handling**: 30-second request timeout

## 📚 API Reference

### Core Types

```rust
pub struct CatFact {
    pub fact: String,
    pub length: usize,
}

pub enum CatFactError {
    Network(String),
    Parse(String),
    InvalidResponse(String),
    Cancelled,
}

pub trait HttpClient: Send + Sync {
    fn get<'a>(&'a self, url: &'a str, headers: Vec<(&'a str, &'a str)>) 
        -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>>;
}

pub struct CatFactService<C: HttpClient> {
    // ...
}

impl<C: HttpClient> CatFactService<C> {
    pub fn new(client: C) -> Self;
    pub fn with_config(client: C, base_url: String, csrf_token: Option<String>) -> Self;
    pub async fn get_random_fact(&self) -> CatFactResult<CatFact>;
}
```

## 🛠️ Development

### Project Structure
- **Workspace**: Cargo workspace with multiple crates
- **Core**: Platform-agnostic business logic
- **Networking**: Platform-specific implementations
- **FFI**: Foreign function interface bindings

### Adding New Features

1. Add domain logic to `crates/core`
2. Add HTTP implementations to `crates/networking`
3. Expose via FFI in `bindings/catfact-ffi`
4. Update UniFFI definition in `catfact.udl`

### Code Quality

```bash
# Format code
cargo fmt

# Lint
cargo clippy -- -D warnings

# Check without building
cargo check --all-targets
```

## 📄 License

MIT License - feel free to use in your projects!

## 🤝 Contributing

Contributions welcome! Please ensure:
- Tests pass: `cargo test`
- Code is formatted: `cargo fmt`
- No clippy warnings: `cargo clippy`

## 🔗 Resources

- [UniFFI Documentation](https://mozilla.github.io/uniffi-rs/)
- [Cat Fact API](https://catfact.ninja)
- [Rust Async Book](https://rust-lang.github.io/async-book/)
- [Tokio Documentation](https://tokio.rs)
