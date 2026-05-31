import initWasm, { CatFactWasmApi } from 'catfact-wasm';
import wasmUrl from 'catfact-wasm/catfact_wasm_bg.wasm?url';

export interface CatFact {
  fact: string;
  length: number;
}

export class CatFactRepository {
  private static instance: CatFactRepository | null = null;
  private api: CatFactWasmApi | null = null;
  private initializedPromise: Promise<void> | null = null;

  private constructor() {}

  public static getInstance(): CatFactRepository {
    if (!CatFactRepository.instance) {
      CatFactRepository.instance = new CatFactRepository();
    }
    return CatFactRepository.instance;
  }

  /**
   * Initializes the WebAssembly module once.
   */
  public async initialize(): Promise<void> {
    if (this.initializedPromise) {
      return this.initializedPromise;
    }

    this.initializedPromise = (async () => {
      try {
        console.log('⚡ Initializing Cat Fact WebAssembly engine...');
        // 1. Initialize the WASM module dynamically using Vite asset loader URL
        await initWasm(wasmUrl);
        // 2. Instantiate the API client
        this.api = new CatFactWasmApi();
        console.log('✅ WebAssembly engine initialized successfully.');
      } catch (error) {
        console.error('❌ Failed to initialize WebAssembly engine:', error);
        this.initializedPromise = null; // Reset to allow retry
        throw error;
      }
    })();

    return this.initializedPromise;
  }

  /**
   * Fetches a random cat fact using the WASM API client.
   */
  public async getRandomFact(): Promise<CatFact> {
    // Make sure WASM is initialized
    await this.initialize();

    if (!this.api) {
      throw new Error('WebAssembly API client is not initialized.');
    }

    try {
      console.log('🐱 Fetching fact from Rust WASM...');
      const fact = await this.api.get_random_fact();
      console.log('📝 Fact retrieved:', fact);
      return fact as CatFact;
    } catch (error: any) {
      console.error('❌ FFI call to get_random_fact failed:', error);
      throw new Error(error?.toString() || 'Failed to fetch fact from WebAssembly engine.');
    }
  }
}
