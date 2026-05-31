/// Example CLI application demonstrating the cat fact API
/// 
/// This shows how to use the core business logic in a native Rust application.

use catfact_core::CatFactService;
use catfact_networking::ReqwestHttpClient;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Create HTTP client with dependency injection
    let client = ReqwestHttpClient::new()?;
    
    // Create service with injected client
    let service = CatFactService::new(client);
    
    println!("🐱 Fetching a random cat fact...\n");
    
    // Fetch a random fact
    let fact = service.get_random_fact().await?;
    
    println!("📝 Fact: {}", fact.fact);
    println!("📏 Length: {} characters", fact.length);
    
    Ok(())
}
