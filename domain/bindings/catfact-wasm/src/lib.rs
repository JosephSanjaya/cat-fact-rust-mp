use wasm_bindgen::prelude::*;
use catfact_core::CatFactService;
use catfact_networking::ReqwestHttpClient;

/// Represents a cat fact in JavaScript
#[derive(serde::Serialize, serde::Deserialize)]
pub struct CatFactJs {
    pub fact: String,
    pub length: u32,
}

/// WebAssembly-compatible Repository for cat facts
#[wasm_bindgen(js_name = CatFactRepository)]
pub struct CatFactWasmRepository {
    service: CatFactService<ReqwestHttpClient>,
}

#[wasm_bindgen(js_class = CatFactRepository)]
impl CatFactWasmRepository {
    /// Create a new instance of the Repository
    #[wasm_bindgen(constructor)]
    pub fn new() -> Result<CatFactWasmRepository, JsValue> {
        // Automatically route panics to console.error
        console_error_panic_hook::set_once();
        
        let client = ReqwestHttpClient::new()
            .map_err(|e| JsValue::from_str(&format!("Failed to create client: {}", e)))?;
            
        Ok(Self {
            service: CatFactService::new(client),
        })
    }

    /// Fetch a random cat fact from the API
    #[wasm_bindgen]
    pub async fn get_random_fact(&self) -> Result<JsValue, JsValue> {
        let fact = self.service.get_random_fact()
            .await
            .map_err(|e| JsValue::from_str(&e.to_string()))?;
            
        let fact_js = CatFactJs {
            fact: fact.fact,
            length: fact.length as u32,
        };
        
        serde_wasm_bindgen::to_value(&fact_js)
            .map_err(|e| JsValue::from_str(&e.to_string()))
    }
}
