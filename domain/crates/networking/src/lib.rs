/// Networking layer with reqwest HTTP client implementation
/// 
/// This crate provides concrete HTTP client implementations for the core service.
/// Platform-specific implementations can be added here or in separate crates.

use catfact_core::HttpClient;
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;

/// Reqwest-based HTTP client implementation
/// 
/// Uses connection pooling and keep-alive for optimal performance.
/// Suitable for native Rust applications (CLI, server, desktop).
pub struct ReqwestHttpClient {
    client: Arc<reqwest::Client>,
}

impl ReqwestHttpClient {
    pub fn new() -> Result<Self, reqwest::Error> {
        #[cfg(not(target_arch = "wasm32"))]
        let builder = reqwest::Client::builder()
            .pool_max_idle_per_host(10)
            .pool_idle_timeout(std::time::Duration::from_secs(90))
            .timeout(std::time::Duration::from_secs(30));

        #[cfg(target_arch = "wasm32")]
        let builder = reqwest::Client::builder();

        let client = builder.build()?;

        Ok(Self {
            client: Arc::new(client),
        })
    }

    /// Create a new client with custom configuration
    pub fn with_client(client: reqwest::Client) -> Self {
        Self {
            client: Arc::new(client),
        }
    }
}

impl Default for ReqwestHttpClient {
    fn default() -> Self {
        Self::new().expect("Failed to create default reqwest client")
    }
}

impl HttpClient for ReqwestHttpClient {
    #[cfg(not(target_arch = "wasm32"))]
    fn get<'a>(
        &'a self,
        url: &'a str,
        headers: Vec<(&'a str, &'a str)>,
    ) -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>> {
        Box::pin(async move {
            let mut request = self.client.get(url);

            for (key, value) in headers {
                request = request.header(key, value);
            }

            let response = request
                .send()
                .await
                .map_err(|e| format!("Request failed: {}", e))?;

            if !response.status().is_success() {
                return Err(format!("HTTP error: {}", response.status()));
            }

            response
                .text()
                .await
                .map_err(|e| format!("Failed to read response: {}", e))
        })
    }

    #[cfg(target_arch = "wasm32")]
    fn get<'a>(
        &'a self,
        url: &'a str,
        headers: Vec<(&'a str, &'a str)>,
    ) -> Pin<Box<dyn Future<Output = Result<String, String>> + 'a>> {
        Box::pin(async move {
            let mut request = self.client.get(url);

            for (key, value) in headers {
                request = request.header(key, value);
            }

            let response = request
                .send()
                .await
                .map_err(|e| format!("Request failed: {}", e))?;

            if !response.status().is_success() {
                return Err(format!("HTTP error: {}", response.status()));
            }

            response
                .text()
                .await
                .map_err(|e| format!("Failed to read response: {}", e))
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use catfact_core::CatFactService;

    #[tokio::test]
    #[ignore] // Requires network access
    async fn test_real_api_call() {
        let client = ReqwestHttpClient::new().unwrap();
        let service = CatFactService::new(client);
        
        let result = service.get_random_fact().await;
        assert!(result.is_ok());
        
        let fact = result.unwrap();
        assert!(!fact.fact.is_empty());
        assert!(fact.length > 0);
    }
}
