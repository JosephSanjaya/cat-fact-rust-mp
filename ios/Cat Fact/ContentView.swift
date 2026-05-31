import SwiftUI

struct ContentView: View {
    // Repository instance
    private let repository = CatFactRepository()
    
    // UI state variables
    @State private var catFact: CatFact? = nil
    @State private var isLoading = false
    @State private var errorMessage: String? = nil
    
    // Animation trigger
    @State private var animateBackground = false
    
    var body: some View {
        ZStack {
            // 1. Premium Dynamic Background: Animated Mesh Gradient
            LinearGradient(
                colors: [
                    Color(red: 0.08, green: 0.07, blue: 0.16), // Deep Space Blue
                    Color(red: 0.18, green: 0.12, blue: 0.28), // Midnight Purple
                    Color(red: 0.12, green: 0.06, blue: 0.22)  // Dark Obsidian
                ],
                startPoint: animateBackground ? .topLeading : .bottomLeading,
                endPoint: animateBackground ? .bottomTrailing : .topTrailing
            )
            .ignoresSafeArea()
            .onAppear {
                withAnimation(.easeInOut(duration: 8.0).repeatForever(autoreverses: true)) {
                    animateBackground.toggle()
                }
            }
            
            // Soft atmospheric lighting overlay
            RadialGradient(
                colors: [
                    Color(red: 0.6, green: 0.35, blue: 0.85).opacity(0.15),
                    Color.clear
                ],
                center: .center,
                startRadius: 0,
                endRadius: 400
            )
            .ignoresSafeArea()
            
            // 2. Main Content Layout
            VStack(spacing: 24) {
                // Header Title
                HStack(spacing: 8) {
                    Text("🐱")
                        .font(.system(size: 32))
                        .rotationEffect(.degrees(isLoading ? 360 : 0))
                        .animation(isLoading ? .linear(duration: 2).repeatForever(autoreverses: false) : .default, value: isLoading)
                    
                    Text("Cat Facts")
                        .font(.system(.title, design: .rounded))
                        .fontWeight(.black)
                        .foregroundStyle(
                            LinearGradient(
                                colors: [.white, Color(red: 0.75, green: 0.65, blue: 0.95)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                }
                .padding(.top, 16)
                
                // 3. Central Glassmorphic Card Container
                ZStack {
                    // Glass Backplate
                    RoundedRectangle(cornerRadius: 30)
                        .fill(.ultraThinMaterial)
                        .overlay(
                            RoundedRectangle(cornerRadius: 30)
                                .stroke(
                                    LinearGradient(
                                        colors: [.white.opacity(0.2), .clear, .white.opacity(0.05)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    lineWidth: 1.5
                                )
                        )
                        .shadow(color: Color.black.opacity(0.3), radius: 20, x: 0, y: 15)
                    
                    // Card Contents
                    Group {
                        if isLoading {
                            // Loading State
                            VStack(spacing: 20) {
                                ProgressView()
                                    .scaleEffect(1.5)
                                    .tint(Color(red: 0.7, green: 0.55, blue: 0.9))
                                
                                Text("Translating Meows...")
                                    .font(.system(.body, design: .rounded))
                                    .fontWeight(.medium)
                                    .foregroundColor(.white.opacity(0.7))
                                    .transition(.opacity)
                            }
                        } else if let errorMessage = errorMessage {
                            // Error State
                            VStack(spacing: 16) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .font(.system(size: 44))
                                    .foregroundColor(.red.opacity(0.8))
                                
                                Text("Connection Lost")
                                    .font(.system(.title3, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                
                                Text(errorMessage)
                                    .font(.system(.body, design: .rounded))
                                    .foregroundColor(.white.opacity(0.7))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 16)
                            }
                            .transition(.scale.combined(with: .opacity))
                        } else if let catFact = catFact {
                            // Fact Display State
                            VStack(spacing: 24) {
                                Text(catFact.fact)
                                    .font(.system(.title3, design: .rounded))
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .multilineTextAlignment(.center)
                                    .lineSpacing(6)
                                    .minimumScaleFactor(0.8)
                                    .padding(.horizontal, 8)
                                
                                // Fact Length Badge
                                HStack(spacing: 6) {
                                    Image(systemName: "clock.fill")
                                        .font(.caption)
                                    Text("\(catFact.length) characters")
                                        .font(.system(.caption, design: .rounded))
                                        .fontWeight(.bold)
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color.white.opacity(0.1))
                                .cornerRadius(12)
                                .foregroundColor(.white.opacity(0.6))
                            }
                            .padding(16)
                            .transition(.asymmetric(
                                insertion: .opacity.combined(with: .move(edge: .bottom)),
                                removal: .opacity.combined(with: .move(edge: .top))
                            ))
                        } else {
                            // Initial State
                            VStack(spacing: 16) {
                                Image(systemName: "sparkles")
                                    .font(.system(size: 48))
                                    .foregroundColor(Color(red: 0.8, green: 0.7, blue: 0.95))
                                    .shadow(color: Color(red: 0.8, green: 0.7, blue: 0.95).opacity(0.5), radius: 10)
                                
                                Text("Ready for Wonders?")
                                    .font(.system(.headline, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                
                                Text("Tap the button below to retrieve an elegant and factual meow from our core Rust-FFI engine.")
                                    .font(.system(.body, design: .rounded))
                                    .foregroundColor(.white.opacity(0.6))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 16)
                            }
                            .transition(.opacity)
                        }
                    }
                    .padding(24)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 320)
                
                // 4. Interactive Gradient Button
                Button(action: {
                    triggerHapticFeedback(style: .medium)
                    fetchFact()
                }) {
                    HStack(spacing: 8) {
                        if isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Image(systemName: "arrow.clockwise")
                                .font(.headline)
                        }
                        
                        Text(isLoading ? "Fetching..." : "Get Random Cat Fact")
                            .font(.system(.headline, design: .rounded))
                            .fontWeight(.bold)
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(
                        LinearGradient(
                            colors: [
                                Color(red: 0.65, green: 0.45, blue: 0.95), // Vibrant Lilac
                                Color(red: 0.45, green: 0.25, blue: 0.85)  // Royal Indigo
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(18)
                    .shadow(color: Color(red: 0.5, green: 0.3, blue: 0.9).opacity(0.4), radius: 12, x: 0, y: 6)
                    .scaleEffect(isLoading ? 0.98 : 1.0)
                    .animation(.spring(), value: isLoading)
                }
                .disabled(isLoading)
                
                // 5. High-fidelity Footer
                VStack(spacing: 4) {
                    Text("Powered by Rust 🦀 via UniFFI")
                        .font(.system(.caption2, design: .monospaced))
                        .fontWeight(.semibold)
                        .foregroundColor(.white.opacity(0.4))
                    
                    Text("Secure TLS via Native Apple Security API")
                        .font(.system(size: 9, design: .rounded))
                        .foregroundColor(.white.opacity(0.25))
                }
                .padding(.bottom, 8)
            }
            .padding(24)
        }
    }
    
    // API Fetch Function
    private func fetchFact() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let fact = try await repository.getRandomFact()
                withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
                    self.catFact = fact
                    self.isLoading = false
                }
                triggerHapticFeedback(style: .success)
            } catch {
                withAnimation(.spring()) {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                    self.catFact = nil
                }
                triggerHapticFeedback(style: .error)
            }
        }
    }
    
    // Haptic Feedback Generator
    private func triggerHapticFeedback(style: UIImpactFeedbackGenerator.FeedbackStyle) {
        let generator = UIImpactFeedbackGenerator(style: style)
        generator.prepare()
        generator.impactOccurred()
    }
    
    private func triggerHapticFeedback(style: UINotificationFeedbackGenerator.FeedbackType) {
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(style)
    }
}

#Preview {
    ContentView()
}
