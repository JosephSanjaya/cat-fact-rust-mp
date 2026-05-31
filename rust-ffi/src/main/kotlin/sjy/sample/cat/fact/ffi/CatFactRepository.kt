package sjy.sample.cat.fact.ffi

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.catfact.ApiConfig
import uniffi.catfact.ApiException
import uniffi.catfact.CatFactApi
import uniffi.catfact.CatFactData

/**
 * Kotlin wrapper for the Rust-based Cat Fact API.
 * 
 * This repository provides a coroutine-friendly interface to the Rust FFI layer,
 * handling thread management and error conversion automatically.
 * 
 * Usage:
 * ```kotlin
 * val repository = CatFactRepository(context)
 * 
 * // Fetch a random cat fact
 * val result = repository.getRandomFact()
 * result.fold(
 *     onSuccess = { fact -> println("Fact: ${fact.fact}") },
 *     onFailure = { error -> println("Error: ${error.message}") }
 * )
 * ```
 */
class CatFactRepository(
    context: Context? = null,
    private val baseUrl: String? = null,
    private val csrfToken: String? = null
) {
    init {
        context?.let { initialize(it) }
    }

    private val api: CatFactApi by lazy {
        val config = ApiConfig(
            baseUrl = baseUrl,
            csrfToken = csrfToken
        )
        CatFactApi.withConfig(config)
    }

    /**
     * Fetch a random cat fact from the API.
     * 
     * This method runs on the IO dispatcher to avoid blocking the main thread.
     * The Rust FFI layer handles the actual HTTP request using its own thread pool.
     * 
     * @return Result containing either a [CatFact] or an error
     */
    suspend fun getRandomFact(): Result<CatFact> = withContext(Dispatchers.IO) {
        try {
            val data = api.getRandomFact()
            Result.success(data.toDomain())
        } catch (e: ApiException.NetworkException) {
            Result.failure(CatFactException.NetworkError(e.reason))
        } catch (e: ApiException.ParseException) {
            Result.failure(CatFactException.ParseError(e.reason))
        } catch (e: ApiException.InvalidResponse) {
            Result.failure(CatFactException.InvalidResponse(e.reason))
        } catch (e: ApiException.Cancelled) {
            Result.failure(CatFactException.Cancelled)
        } catch (e: Exception) {
            Result.failure(CatFactException.Unknown(e.message ?: "Unknown error"))
        }
    }

    /**
     * Fetch a random cat fact synchronously (blocking).
     * 
     * ⚠️ This method blocks the calling thread. Use [getRandomFact] for coroutine-based calls.
     * 
     * @return Result containing either a [CatFact] or an error
     */
    fun getRandomFactBlocking(): Result<CatFact> {
        return try {
            val data = api.getRandomFact()
            Result.success(data.toDomain())
        } catch (e: ApiException.NetworkException) {
            Result.failure(CatFactException.NetworkError(e.reason))
        } catch (e: ApiException.ParseException) {
            Result.failure(CatFactException.ParseError(e.reason))
        } catch (e: ApiException.InvalidResponse) {
            Result.failure(CatFactException.InvalidResponse(e.reason))
        } catch (e: ApiException.Cancelled) {
            Result.failure(CatFactException.Cancelled)
        } catch (e: Exception) {
            Result.failure(CatFactException.Unknown(e.message ?: "Unknown error"))
        }
    }

    private fun CatFactData.toDomain() = CatFact(
        fact = this.fact,
        length = this.length.toInt()
    )

    companion object {
        private var isInitialized = false

        @JvmStatic
        fun initialize(context: Context) {
            if (isInitialized) return
            try {
                System.loadLibrary("uniffi_catfact")
                initPlatformVerifier(context.applicationContext)
                isInitialized = true
            } catch (e: Throwable) {
                android.util.Log.w("CatFactRepository", "Failed to initialize platform verifier: ${e.message}", e)
            }
        }

        @JvmStatic
        private external fun initPlatformVerifier(context: Any)
    }
}

/**
 * Domain model for a cat fact.
 * 
 * This is a Kotlin-friendly wrapper around the FFI [CatFactData] type.
 */
data class CatFact(
    val fact: String,
    val length: Int
)

/**
 * Sealed hierarchy of exceptions that can occur when fetching cat facts.
 */
sealed class CatFactException(message: String) : Exception(message) {
    /**
     * Network-related error (connection failed, timeout, etc.)
     */
    data class NetworkError(override val message: String) : CatFactException(message)

    /**
     * Failed to parse the API response
     */
    data class ParseError(override val message: String) : CatFactException(message)

    /**
     * API returned an invalid or unexpected response
     */
    data class InvalidResponse(override val message: String) : CatFactException(message)

    /**
     * The operation was cancelled
     */
    data object Cancelled : CatFactException("Operation cancelled")

    /**
     * Unknown error occurred
     */
    data class Unknown(override val message: String) : CatFactException(message)
}
