# Architecture Documentation

## Overview

This project demonstrates a production-ready multiplatform Rust architecture following SOLID principles, dependency injection, and composition over inheritance.

## Design Patterns

### 1. Dependency Injection (DI)

The `HttpClient` trait serves as an abstraction boundary:

```rust
pub trait HttpClient: Send + Sync {
    fn get<'a>(&'a self, url: &'a str, headers: Vec<(&'a str, &'a str)>) 
        -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>>;
}
```

**Benefits:**
- **Testability**: Inject mock clients for unit tests
- **Platform flexibility**: Different implementations per platform
- **Loose coupling**: Core logic doesn't depend on concrete HTTP libraries

**Implementations:**
- `ReqwestHttpClient`: Native Rust (CLI, server, desktop)
- `MockHttpClient`: Testing
- Future: `URLSessionClient` (iOS), `OkHttpClient` (Android), `FetchClient` (Web)

### 2. Composition Over Inheritance

Instead of inheritance hierarchies, we compose behavior:

```rust
pub struct CatFactService<C: HttpClient> {
    client: C,
    base_url: String,
    csrf_token: Option<String>,
}
```

The service **composes** an HTTP client rather than inheriting from a base class. This provides:
- **Flexibility**: Swap implementations at runtime
- **Simplicity**: No complex inheritance chains
- **Type safety**: Compile-time guarantees via generics

### 3. Layered Architecture

```
┌─────────────────────────────────────┐
│         FFI Layer (UniFFI)          │  ← Platform bindings
│  Android (Kotlin) | iOS (Swift)     │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Networking Layer (Reqwest)     │  ← HTTP implementations
│   ReqwestHttpClient, URLSession...  │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Core Business Logic            │  ← Pure domain logic
│   CatFactService, Models, Errors    │
└─────────────────────────────────────┘
```

**Layer Responsibilities:**

1. **Core Layer** (`crates/core`)
   - Pure business logic
   - No I/O operations
   - Platform-agnostic
   - Defines abstractions (traits)

2. **Networking Layer** (`crates/networking`)
   - Concrete HTTP implementations
   - Platform-specific optimizations
   - Connection pooling, timeouts

3. **FFI Layer** (`bindings/catfact-ffi`)
   - Foreign function interface
   - Runtime management (Tokio)
   - Platform-specific logging
   - Type conversions

### 4. Error Handling Strategy

**Core Layer** uses `thiserror` for library errors:
```rust
#[derive(Debug, thiserror::Error)]
pub enum CatFactError {
    #[error("Network error: {0}")]
    Network(String),
    // ...
}
```

**FFI Layer** converts to UniFFI-compatible errors:
```rust
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ApiError {
    #[error("Network error: {message}")]
    NetworkError { message: String },
    // ...
}
```

This separation ensures:
- Core errors are Rust-idiomatic
- FFI errors are foreign-language friendly
- Clear error propagation boundaries

## Concurrency Model

### Async Runtime

The FFI layer manages a Tokio runtime:
```rust
let runtime = tokio::runtime::Builder::new_multi_thread()
    .worker_threads(2)
    .thread_name("catfact-worker")
    .enable_all()
    .build()?;
```

**Design decisions:**
- **2 worker threads**: Sufficient for I/O-bound HTTP requests
- **Multi-threaded**: Better than single-threaded for mobile
- **Named threads**: Easier debugging in profilers

### Blocking FFI Bridge

FFI calls are synchronous (required by UniFFI):
```rust
pub fn get_random_fact(&self) -> Result<CatFactData, ApiError> {
    self.runtime.block_on(self.service.get_random_fact())
        .map_err(Into::into)
}
```

This bridges async Rust with synchronous foreign languages.

## Memory Management

### Reference Counting

The FFI layer uses `Arc` for thread-safe sharing:
```rust
pub struct CatFactApi {
    runtime: Arc<tokio::runtime::Runtime>,
    service: Arc<CatFactService<ReqwestHttpClient>>,
}
```

**Benefits:**
- Thread-safe cloning
- Efficient memory usage
- Automatic cleanup when last reference drops

### FFI Lifecycle

UniFFI generates platform-specific lifecycle management:
- **Kotlin**: Implements `AutoCloseable`
- **Swift**: Uses ARC (Automatic Reference Counting)
- **Python**: Uses context managers

## Testing Strategy

### Unit Tests (Core)

Mock the HTTP client:
```rust
struct MockHttpClient {
    response: Arc<str>,
}

impl HttpClient for MockHttpClient {
    // Return canned responses
}
```

### Integration Tests (Networking)

Test real HTTP calls (marked `#[ignore]`):
```rust
#[tokio::test]
#[ignore] // Requires network
async fn test_real_api_call() {
    let client = ReqwestHttpClient::new().unwrap();
    let service = CatFactService::new(client);
    let result = service.get_random_fact().await;
    assert!(result.is_ok());
}
```

### FFI Tests

Test the FFI boundary:
```rust
#[test]
fn test_api_creation() {
    let api = CatFactApi::new();
    assert!(api.is_ok());
}
```

## Performance Considerations

### Connection Pooling

Reqwest maintains a connection pool:
```rust
let client = reqwest::Client::builder()
    .pool_max_idle_per_host(10)
    .pool_idle_timeout(Duration::from_secs(90))
    .build()?;
```

This reuses TCP connections, reducing latency.

### Release Optimizations

```toml
[profile.release]
opt-level = 3          # Maximum optimization
lto = true             # Link-time optimization
codegen-units = 1      # Single codegen unit for max optimization
strip = true           # Remove debug symbols (~40% size reduction)
panic = "unwind"       # Required for FFI safety
```

### Binary Size

Typical sizes (release, stripped):
- **iOS**: ~2-3 MB per architecture
- **Android**: ~2-3 MB per architecture
- **CLI**: ~3-4 MB

## Platform-Specific Considerations

### Android

- **Logging**: Uses `android_logger` for logcat integration
- **JNI**: UniFFI generates JNI bindings automatically
- **ProGuard**: Keep rules needed for reflection

### iOS

- **Logging**: Uses `oslog` for unified logging
- **XCFramework**: Bundles multiple architectures
- **Swift**: Automatic memory management via ARC

### Web (WASM)

- **Future**: Replace Tokio with wasm-bindgen-futures
- **HTTP**: Use browser's Fetch API
- **Size**: Optimize with wasm-opt

## Extending the Architecture

### Adding New Endpoints

1. Add method to `CatFactService`:
```rust
impl<C: HttpClient> CatFactService<C> {
    pub async fn get_facts_by_length(&self, max_length: usize) 
        -> CatFactResult<Vec<CatFact>> {
        // Implementation
    }
}
```

2. Expose via FFI:
```rust
#[uniffi::export]
impl CatFactApi {
    pub fn get_facts_by_length(&self, max_length: u64) 
        -> Result<Vec<CatFactData>, ApiError> {
        self.runtime.block_on(
            self.service.get_facts_by_length(max_length as usize)
        ).map_err(Into::into)
    }
}
```

3. Update UniFFI definition:
```udl
interface CatFactApi {
    [Throws=ApiError]
    sequence<CatFactData> get_facts_by_length(u64 max_length);
};
```

### Adding Platform-Specific HTTP Clients

1. Implement `HttpClient` trait:
```rust
pub struct URLSessionClient {
    // iOS-specific implementation
}

impl HttpClient for URLSessionClient {
    fn get<'a>(&'a self, url: &'a str, headers: Vec<(&'a str, &'a str)>) 
        -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>> {
        // Delegate to iOS URLSession via FFI
    }
}
```

2. Use conditional compilation:
```rust
#[cfg(target_os = "ios")]
let client = URLSessionClient::new();

#[cfg(not(target_os = "ios"))]
let client = ReqwestHttpClient::new()?;
```

## Security Considerations

### HTTPS Only

Reqwest uses `rustls-tls` (no OpenSSL dependency):
```toml
reqwest = { version = "0.12", features = ["json", "rustls-tls"] }
```

### Certificate Pinning (Future)

Add to `ReqwestHttpClient`:
```rust
let client = reqwest::Client::builder()
    .add_root_certificate(cert)
    .build()?;
```

### Input Validation

Validate responses:
```rust
if fact.fact.is_empty() {
    return Err(CatFactError::InvalidResponse("Fact is empty".to_string()));
}
```

## Monitoring and Observability

### Logging

Platform-specific loggers:
- **Android**: `android_logger` → logcat
- **iOS**: `oslog` → Console.app
- **CLI**: `env_logger` → stderr

### Metrics (Future)

Add instrumentation:
```rust
use metrics::{counter, histogram};

counter!("api.requests.total").increment(1);
histogram!("api.request.duration").record(duration);
```

## Conclusion

This architecture provides:
- ✅ **Testability**: Mock-friendly DI
- ✅ **Maintainability**: Clear separation of concerns
- ✅ **Extensibility**: Easy to add features
- ✅ **Performance**: Optimized for production
- ✅ **Multiplatform**: Single codebase, multiple targets
- ✅ **Type Safety**: Compile-time guarantees

The key insight is **composition over inheritance** combined with **dependency injection** creates a flexible, testable architecture that scales across platforms.
