use serde::{Deserialize, Serialize};

/// Represents a cat fact returned from the API
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct CatFact {
    /// The cat fact text
    pub fact: String,
    /// Length of the fact in characters
    pub length: usize,
}

/// Domain errors for cat fact operations
#[derive(Debug, thiserror::Error)]
pub enum CatFactError {
    #[error("Network error: {0}")]
    Network(String),
    
    #[error("Parse error: {0}")]
    Parse(String),
    
    #[error("Invalid response: {0}")]
    InvalidResponse(String),
    
    #[error("Operation cancelled")]
    Cancelled,
}

/// Result type for cat fact operations
pub type CatFactResult<T> = Result<T, CatFactError>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_catfact_serialization() {
        let fact = CatFact {
            fact: "Cats are awesome".to_string(),
            length: 16,
        };
        
        let json = serde_json::to_string(&fact).unwrap();
        let deserialized: CatFact = serde_json::from_str(&json).unwrap();
        
        assert_eq!(fact, deserialized);
    }
}
