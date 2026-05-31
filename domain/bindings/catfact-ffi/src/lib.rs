/// FFI bindings for Android, iOS, and other platforms via UniFFI
/// 
/// This crate exposes the core business logic through a C-compatible FFI
/// with automatic bindings generation for Kotlin, Swift, and Python.

use catfact_core::{CatFactError, CatFactService};
use catfact_networking::ReqwestHttpClient;
use std::sync::Arc;
use std::sync::LazyLock;

static GLOBAL_RUNTIME: LazyLock<tokio::runtime::Runtime> = LazyLock::new(|| {
    tokio::runtime::Builder::new_multi_thread()
        .worker_threads(2)
        .thread_name("catfact-worker")
        .enable_all()
        .build()
        .expect("Failed to create global Tokio runtime")
});

/// Guard to abort a task when dropped (for cooperative cancellation)
struct AbortOnDrop(tokio::task::AbortHandle);

impl Drop for AbortOnDrop {
    fn drop(&mut self) {
        self.0.abort();
    }
}

/// FFI-safe domain model
#[derive(uniffi::Record)]
pub struct CatFactData {
    pub fact: String,
    pub length: u64,
}

/// FFI-safe error type
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ApiError {
    #[error("Network error: {reason}")]
    NetworkError { reason: String },
    
    #[error("Parse error: {reason}")]
    ParseError { reason: String },
    
    #[error("Invalid response: {reason}")]
    InvalidResponse { reason: String },
    
    #[error("Operation cancelled")]
    Cancelled,
}

impl From<CatFactError> for ApiError {
    fn from(error: CatFactError) -> Self {
        match error {
            CatFactError::Network(msg) => ApiError::NetworkError { reason: msg },
            CatFactError::Parse(msg) => ApiError::ParseError { reason: msg },
            CatFactError::InvalidResponse(msg) => ApiError::InvalidResponse { reason: msg },
            CatFactError::Cancelled => ApiError::Cancelled,
        }
    }
}

/// Configuration for the cat fact API client
#[derive(uniffi::Record)]
pub struct ApiConfig {
    pub base_url: Option<String>,
    pub csrf_token: Option<String>,
}

/// Main Repository client exposed to foreign languages
/// 
/// Thread-safe and can be shared across multiple threads.
/// Uses Arc internally for efficient cloning.
#[derive(uniffi::Object)]
pub struct CatFactRepository {
    service: Arc<CatFactService<ReqwestHttpClient>>,
}

#[uniffi::export]
impl CatFactRepository {
    /// Create a new Repository client with default configuration
    #[uniffi::constructor]
    pub fn new() -> Result<Arc<Self>, ApiError> {
        Self::with_config(ApiConfig {
            base_url: None,
            csrf_token: None,
        })
    }

    /// Create a new Repository client with custom configuration
    #[uniffi::constructor]
    pub fn with_config(config: ApiConfig) -> Result<Arc<Self>, ApiError> {
        // Initialize platform-specific logging
        #[cfg(target_os = "android")]
        {
            android_logger::init_once(
                android_logger::Config::default()
                    .with_max_level(log::LevelFilter::Info)
                    .with_tag("CatFactAPI"),
            );
        }

        #[cfg(target_os = "ios")]
        {
            oslog::OsLogger::new("com.catfact.api")
                .level_filter(log::LevelFilter::Info)
                .init()
                .ok();
        }

        let client = ReqwestHttpClient::new().map_err(|e| ApiError::NetworkError {
            reason: format!("Failed to create HTTP client: {}", e),
        })?;

        let service = if let Some(base_url) = config.base_url {
            CatFactService::with_config(client, base_url, config.csrf_token)
        } else {
            CatFactService::new(client)
        };

        Ok(Arc::new(Self {
            service: Arc::new(service),
        }))
    }

    /// Fetch a random cat fact asynchronously
    pub async fn get_random_fact(&self) -> Result<CatFactData, ApiError> {
        let service = self.service.clone();
        
        let handle = GLOBAL_RUNTIME.spawn(async move {
            service.get_random_fact().await
        });
        
        // Attach cancellation guard
        let abort_handle = handle.abort_handle();
        let _abort_on_drop = AbortOnDrop(abort_handle);
        
        let core_fact = handle.await
            .map_err(|e| ApiError::NetworkError {
                reason: format!("Task execution failed: {}", e),
            })?
            .map_err(ApiError::from)?;
            
        Ok(CatFactData {
            fact: core_fact.fact,
            length: core_fact.length as u64,
        })
    }
}

#[cfg(target_os = "android")]
#[no_mangle]
pub extern "system" fn Java_sjy_sample_cat_fact_ffi_CatFactRepository_initPlatformVerifier(
    mut env: jni::EnvUnowned,
    _class: jni::objects::JClass,
    context: jni::objects::JObject,
) {
    let _ = env.with_env(|env_ref| {
        rustls_platform_verifier::android::init_with_env(env_ref, context)
    });
}

// Generate FFI scaffolding
uniffi::setup_scaffolding!();

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_api_creation() {
        let repository = CatFactRepository::new();
        assert!(repository.is_ok());
    }

    #[test]
    fn test_api_with_custom_config() {
        let config = ApiConfig {
            base_url: Some("https://catfact.ninja".to_string()),
            csrf_token: Some("test-token".to_string()),
        };
        
        let repository = CatFactRepository::with_config(config);
        assert!(repository.is_ok());
    }

    #[tokio::test]
    #[ignore] // Requires network access
    async fn test_get_random_fact() {
        let repository = CatFactRepository::new().unwrap();
        let result = repository.get_random_fact().await;
        
        assert!(result.is_ok());
        let fact = result.unwrap();
        assert!(!fact.fact.is_empty());
    }
}
