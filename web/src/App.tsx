import React, { useState, useEffect } from 'react';
import { CatFactRepository, CatFact } from './CatFactRepository';
import './App.css';

export const App: React.FC = () => {
  const repository = CatFactRepository.getInstance();

  // UI state variables
  const [catFact, setCatFact] = useState<CatFact | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isWasmReady, setIsWasmReady] = useState<boolean>(false);

  // Initialize WebAssembly engine on mount
  useEffect(() => {
    async function init() {
      try {
        await repository.initialize();
        setIsWasmReady(true);
      } catch (err) {
        setErrorMessage('Failed to initialize Cat Fact WebAssembly engine.');
      }
    }
    init();
  }, []);

  // API Fetch Function
  const fetchFact = async () => {
    if (isLoading) return;

    setIsLoading(true);
    setErrorMessage(null);
    triggerTactileFeedback('medium');

    try {
      const fact = await repository.getRandomFact();
      setCatFact(fact);
      triggerTactileFeedback('success');
    } catch (err: any) {
      setErrorMessage(err?.message || 'Connection Lost');
      setCatFact(null);
      triggerTactileFeedback('error');
    } finally {
      setIsLoading(false);
    }
  };

  // Tactile/Haptic Feedback Emulation
  const triggerTactileFeedback = (type: 'medium' | 'success' | 'error') => {
    if (!('vibrate' in navigator)) return;
    
    switch (type) {
      case 'medium':
        navigator.vibrate(20);
        break;
      case 'success':
        navigator.vibrate([15, 30, 20]);
        break;
      case 'error':
        navigator.vibrate([40, 60, 40]);
        break;
    }
  };

  return (
    <div className="app-container">
      {/* Soft atmospheric lighting overlay */}
      <div className="atmospheric-glow" />

      {/* Main Content Layout */}
      <main className="content-layout">
        {/* Header Title */}
        <header className="header">
          <span 
            className={`cat-emoji ${isLoading ? 'spinning' : ''}`}
            aria-hidden="true"
          >
            🐱
          </span>
          <h1 className="title-gradient">Cat Facts</h1>
        </header>

        {/* Central Glassmorphic Card Container */}
        <section className="glass-card">
          <div className="card-border-glow" />
          <div className="card-content">
            {!isWasmReady && !errorMessage ? (
              // WASM loading state
              <div className="card-state loading-state">
                <div className="progress-spinner" />
                <p className="state-message">Warming up Rust core...</p>
              </div>
            ) : isLoading ? (
              // Fetching State
              <div className="card-state loading-state">
                <div className="progress-spinner" />
                <p className="state-message">Translating Meows...</p>
              </div>
            ) : errorMessage ? (
              // Error State
              <div className="card-state error-state">
                <span className="error-icon" aria-hidden="true">⚠️</span>
                <h3 className="error-title">Connection Lost</h3>
                <p className="error-description">{errorMessage}</p>
              </div>
            ) : catFact ? (
              // Fact Display State
              <div className="card-state fact-state">
                <blockquote className="fact-text">
                  "{catFact.fact}"
                </blockquote>
                
                {/* Fact Length Badge */}
                <div className="char-badge">
                  <span className="badge-icon">⏱️</span>
                  <span>{catFact.length} characters</span>
                </div>
              </div>
            ) : (
              // Initial State
              <div className="card-state initial-state">
                <div className="sparkle-glow">✨</div>
                <h3 className="initial-title">Ready for Wonders?</h3>
                <p className="initial-description">
                  Tap the button below to retrieve an elegant and factual meow from our core Rust-WASM engine.
                </p>
              </div>
            )}
          </div>
        </section>

        {/* Interactive Gradient Button */}
        <button
          onClick={fetchFact}
          disabled={isLoading || (!isWasmReady && !errorMessage)}
          className={`btn-gradient ${isLoading ? 'btn-active' : ''}`}
        >
          <div className="btn-content">
            {isLoading ? (
              <div className="btn-spinner" />
            ) : (
              <span className="btn-icon">🔄</span>
            )}
            <span>
              {!isWasmReady && !errorMessage 
                ? 'Initializing...' 
                : isLoading 
                  ? 'Fetching...' 
                  : 'Get Random Cat Fact'}
            </span>
          </div>
        </button>

        {/* High-fidelity Footer */}
        <footer className="footer">
          <p className="footer-line1">Powered by Rust 🦀 via WebAssembly</p>
          <p className="footer-line2">Secure TLS via Browser Native Fetch Engine</p>
        </footer>
      </main>
    </div>
  );
};
export default App;
