# catfact-wasm

WebAssembly bindings for the Cat Fact API, enabling high-performance, single-threaded execution of core Rust business logic directly in browser runtimes.

This package is optimized for modern ES module environments and can be effortlessly integrated into modern frontend frameworks like **React**, **Vue**, or **Svelte** (e.g., using **Vite**).

---

## 🛠️ Building the WASM Package

The build is fully automated. To compile and generate the WebAssembly module along with its JavaScript and TypeScript bindings, run the following command from the root `domain` directory:

```bash
./scripts/build-wasm.sh
```

This compiles the Rust crate and outputs a complete, ready-to-use npm package in `bindings/catfact-wasm/pkg`.

---

## 📦 How to Consume in React (Vite)

Because this package is compiled with `--target web`, it generates standard ES modules. You do not need to install complex Vite/Webpack plugins or configure custom WASM loaders!

### 1. Install or Reference the Package
In your React project's `package.json`, you can directly reference the local package:

```json
"dependencies": {
  "catfact-wasm": "file:../path/to/cat-fact/domain/bindings/catfact-wasm/pkg"
}
```

Then run `npm install` (or `yarn install` / `pnpm install`).

### 2. Premium React Component Integration

Below is a complete, premium-quality React component implementing glassmorphism, responsive states, micro-animations, and dynamic loading feedback:

```tsx
import React, { useState, useEffect, useRef } from 'react';
import initWasm, { CatFactWasmApi, CatFact } from 'catfact-wasm';

export const CatFactDisplay: React.FC = () => {
  const [fact, setFact] = useState<string>('');
  const [length, setLength] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  // Keep a reference to the API client to avoid re-instantiating
  const apiRef = useRef<CatFactWasmApi | null>(null);

  useEffect(() => {
    async function setupWasm() {
      try {
        // 1. Initialize the WebAssembly module
        await initWasm();
        
        // 2. Instantiate the API client
        apiRef.current = new CatFactWasmApi();
        
        // 3. Fetch the first fact
        await fetchFact();
      } catch (err) {
        console.error('Failed to load WASM:', err);
        setError('Failed to initialize Cat Fact WebAssembly engine.');
        setLoading(false);
      }
    }
    setupWasm();
  }, []);

  const fetchFact = async () => {
    if (!apiRef.current) return;
    
    setLoading(true);
    setError(null);
    
    try {
      // 3. Invoke the Rust FFI async function
      const result = (await apiRef.current.get_random_fact()) as CatFact;
      setFact(result.fact);
      setLength(result.length);
    } catch (err: any) {
      setError(err?.toString() || 'Failed to fetch a new fact.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="cat-fact-container">
      {/* Premium Frosted Glass Card Design */}
      <div className="glass-card">
        <div className="card-header">
          <span className="badge">🐱 Rust WASM Powered</span>
          <h2>Random Cat Fact</h2>
        </div>

        <div className="card-content">
          {loading ? (
            <div className="skeleton-loader">
              <div className="skeleton-line" />
              <div className="skeleton-line short" />
            </div>
          ) : error ? (
            <div className="error-alert">
              <span className="error-icon">⚠️</span>
              <p>{error}</p>
            </div>
          ) : (
            <blockquote className="fact-quote">
              "{fact}"
              {length > 0 && <cite>Length: {length} chars</cite>}
            </blockquote>
          )}
        </div>

        <div className="card-actions">
          <button 
            onClick={fetchFact} 
            disabled={loading}
            className={`btn-fetch ${loading ? 'anim-pulse' : ''}`}
          >
            {loading ? 'Fetching...' : 'Show Another Fact'}
          </button>
        </div>
      </div>

      <style>{`
        .cat-fact-container {
          display: flex;
          justify-content: center;
          align-items: center;
          padding: 2rem;
          background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);
          min-height: 350px;
          border-radius: 16px;
          font-family: 'Outfit', 'Inter', system-ui, sans-serif;
        }

        .glass-card {
          background: rgba(255, 255, 255, 0.03);
          backdrop-filter: blur(12px);
          -webkit-backdrop-filter: blur(12px);
          border: 1px solid rgba(255, 255, 255, 0.08);
          border-radius: 24px;
          padding: 2rem;
          max-width: 450px;
          width: 100%;
          box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
          transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        .glass-card:hover {
          transform: translateY(-4px);
          box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.45);
        }

        .badge {
          font-size: 0.75rem;
          font-weight: 600;
          color: #818cf8;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          background: rgba(129, 140, 248, 0.1);
          padding: 0.25rem 0.75rem;
          border-radius: 9999px;
        }

        h2 {
          color: #f8fafc;
          margin: 0.75rem 0 1.5rem 0;
          font-weight: 700;
          font-size: 1.75rem;
        }

        .fact-quote {
          color: #e2e8f0;
          font-size: 1.125rem;
          line-height: 1.6;
          margin: 0;
          font-style: italic;
          min-height: 80px;
          display: flex;
          flex-direction: column;
          justify-content: center;
        }

        cite {
          display: block;
          margin-top: 1rem;
          font-size: 0.85rem;
          color: #94a3b8;
          font-style: normal;
          font-weight: 500;
        }

        .error-alert {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          color: #f87171;
          background: rgba(248, 113, 113, 0.08);
          padding: 1rem;
          border-radius: 12px;
          border: 1px solid rgba(248, 113, 113, 0.2);
        }

        .btn-fetch {
          background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
          color: white;
          border: none;
          padding: 0.875rem 1.75rem;
          font-size: 1rem;
          font-weight: 600;
          border-radius: 14px;
          cursor: pointer;
          width: 100%;
          transition: filter 0.2s ease, transform 0.1s ease;
          box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
        }

        .btn-fetch:hover:not(:disabled) {
          filter: brightness(1.1);
        }

        .btn-fetch:active:not(:disabled) {
          transform: scale(0.98);
        }

        .btn-fetch:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        /* Skeleton Animations */
        .skeleton-loader {
          display: flex;
          flex-direction: column;
          gap: 0.75rem;
          height: 80px;
          justify-content: center;
        }

        .skeleton-line {
          height: 14px;
          background: rgba(255, 255, 255, 0.06);
          border-radius: 4px;
          animation: pulse 1.5s infinite ease-in-out;
        }

        .skeleton-line.short {
          width: 60%;
        }

        @keyframes pulse {
          0%, 100% { opacity: 0.6; }
          50% { opacity: 0.3; }
        }

        .anim-pulse {
          animation: pulse 1.5s infinite ease-in-out;
        }
      `}</style>
    </div>
  );
};
```
