use crate::{CatFact, CatFactResult};
use std::future::Future;
use std::pin::Pin;

/// Trait for HTTP client abstraction (dependency injection)
/// 
/// This allows platform-specific implementations:
/// - Rust native: reqwest
/// - iOS: URLSession via FFI delegation
/// - Android: OkHttp via FFI delegation
/// - Web: fetch API via wasm-bindgen
pub trait HttpClient: Send + Sync {
    /// Perform a GET request and return the response body
    fn get<'a>(
        &'a self,
        url: &'a str,
        headers: Vec<(&'a str, &'a str)>,
    ) -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>>;
}

/// Core business logic service for cat facts
/// 
/// Uses dependency injection for HTTP client to enable:
/// - Easy testing with mock clients
/// - Platform-specific HTTP implementations
/// - Composition over inheritance
pub struct CatFactService<C: HttpClient> {
    client: C,
    base_url: String,
    csrf_token: Option<String>,
}

impl<C: HttpClient> CatFactService<C> {
    /// Create a new service with injected HTTP client
    pub fn new(client: C) -> Self {
        Self {
            client,
            base_url: "https://catfact.ninja".to_string(),
            csrf_token: None,
        }
    }

    /// Create a new service with custom base URL and CSRF token
    pub fn with_config(client: C, base_url: String, csrf_token: Option<String>) -> Self {
        Self {
            client,
            base_url,
            csrf_token,
        }
    }

    /// Fetch a random cat fact
    pub async fn get_random_fact(&self) -> CatFactResult<CatFact> {
        let url = format!("{}/fact", self.base_url);
        
        let mut headers = vec![("accept", "application/json")];
        
        if let Some(token) = &self.csrf_token {
            headers.push(("X-CSRF-TOKEN", token.as_str()));
        }

        let response = self
            .client
            .get(&url, headers)
            .await
            .map_err(|e| crate::CatFactError::Network(e))?;

        let fact: CatFact = serde_json::from_str(&response)
            .map_err(|e| crate::CatFactError::Parse(e.to_string()))?;

        // Validate the response
        if fact.fact.is_empty() {
            return Err(crate::CatFactError::InvalidResponse(
                "Fact is empty".to_string(),
            ));
        }

        Ok(fact)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Arc;

    /// Mock HTTP client for testing
    struct MockHttpClient {
        response: Arc<str>,
    }

    impl HttpClient for MockHttpClient {
        fn get<'a>(
            &'a self,
            _url: &'a str,
            _headers: Vec<(&'a str, &'a str)>,
        ) -> Pin<Box<dyn Future<Output = Result<String, String>> + Send + 'a>> {
            let response = self.response.clone();
            Box::pin(async move { Ok(response.to_string()) })
        }
    }

    #[tokio::test]
    async fn test_get_random_fact_success() {
        let mock_response = r#"{"fact":"Cats are great","length":14}"#;
        let client = MockHttpClient {
            response: Arc::from(mock_response),
        };
        
        let service = CatFactService::new(client);
        let result = service.get_random_fact().await;
        
        assert!(result.is_ok());
        let fact = result.unwrap();
        assert_eq!(fact.fact, "Cats are great");
        assert_eq!(fact.length, 14);
    }

    #[tokio::test]
    async fn test_get_random_fact_empty_response() {
        let mock_response = r#"{"fact":"","length":0}"#;
        let client = MockHttpClient {
            response: Arc::from(mock_response),
        };
        
        let service = CatFactService::new(client);
        let result = service.get_random_fact().await;
        
        assert!(result.is_err());
        match result {
            Err(crate::CatFactError::InvalidResponse(_)) => {},
            _ => panic!("Expected InvalidResponse error"),
        }
    }
}
