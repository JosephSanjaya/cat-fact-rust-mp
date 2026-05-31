package sjy.sample.cat.fact.ffi

import android.content.Context
import uniffi.catfact.ApiConfig
import uniffi.catfact.CatFactRepository as RustRepository

/**
 * Kotlin repository wrapping the Rust CatFactRepository.
 * 
 * Provides native coroutine bindings and handles the JNI NDK
 * TLS verifier initialization on Android.
 */
class CatFactRepository(
    context: Context? = null,
    private val baseUrl: String? = null,
    private val csrfToken: String? = null
) {
    init {
        context?.let { initialize(it) }
    }

    private val delegate: RustRepository by lazy {
        val config = ApiConfig(
            baseUrl = baseUrl,
            csrfToken = csrfToken
        )
        RustRepository.withConfig(config)
    }

    /**
     * Fetch a random cat fact natively using the async FFI client.
     */
    suspend fun getRandomFact(): uniffi.catfact.CatFactData {
        return delegate.getRandomFact()
    }

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
