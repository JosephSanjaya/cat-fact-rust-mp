import Foundation

/// Domain model representing a cat fact.
public struct CatFact: Equatable, Hashable, Identifiable {
    public var id = UUID()
    public let fact: String
    public let length: Int
    
    public init(fact: String, length: Int) {
        self.fact = fact
        self.length = length
    }
}

/// Structured exceptions that can occur when calling the Cat Fact API.
public enum CatFactError: Error, LocalizedError, Equatable {
    case networkError(reason: String)
    case parseError(reason: String)
    case invalidResponse(reason: String)
    case cancelled
    case unknown(reason: String)
    
    public var errorDescription: String? {
        switch self {
        case .networkError(let reason):
            return "Network Error: \(reason)"
        case .parseError(let reason):
            return "Failed to parse API response: \(reason)"
        case .invalidResponse(let reason):
            return "Invalid API response: \(reason)"
        case .cancelled:
            return "Operation cancelled"
        case .unknown(let reason):
            return "An unexpected error occurred: \(reason)"
        }
    }
}

/// A thread-safe, coroutine-friendly Repository wrapping the Rust FFI Cat Fact API client.
/// Uses Swift Structured Concurrency to isolate blocking FFI calls on background threads.
public final class CatFactRepository: Sendable {
    
    private let baseUrl: String?
    private let csrfToken: String?
    
    /// Initialize a new CatFactRepository with optional configuration.
    ///
    /// - Parameters:
    ///   - baseUrl: Optional custom base URL for the API
    ///   - csrfToken: Optional CSRF token for request validation
    public init(baseUrl: String? = nil, csrfToken: String? = nil) {
        self.baseUrl = baseUrl
        self.csrfToken = csrfToken
    }
    
    /// Fetches a random cat fact asynchronously.
    /// Spawns the blocking Rust FFI call on a background thread pool to ensure UI responsiveness.
    ///
    /// - Returns: A `CatFact` instance on success
    /// - Throws: A mapped `CatFactError` on failure
    public func getRandomFact() async throws -> CatFact {
        // Capture configuration parameters for the detached task
        let url = self.baseUrl
        let token = self.csrfToken
        
        return try await Task.detached(priority: .userInitiated) {
            do {
                // Initialize the API client with custom or default config
                let config = ApiConfig(baseUrl: url, csrfToken: token)
                let apiClient = try CatFactApi.withConfig(config: config)
                
                // Invoke FFI call (which blocks the background thread)
                let data = try apiClient.getRandomFact()
                
                // Map to domain model
                return CatFact(fact: data.fact, length: Int(data.length))
                
            } catch let error as ApiError {
                // Map UniFFI errors directly to native domain errors
                switch error {
                case .NetworkError(let reason):
                    throw CatFactError.networkError(reason: reason)
                case .ParseError(let reason):
                    throw CatFactError.parseError(reason: reason)
                case .InvalidResponse(let reason):
                    throw CatFactError.invalidResponse(reason: reason)
                case .Cancelled:
                    throw CatFactError.cancelled
                }
            } catch {
                throw CatFactError.unknown(reason: error.localizedDescription)
            }
        }.value
    }
}
