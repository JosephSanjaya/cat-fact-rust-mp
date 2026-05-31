import java.io.ByteArrayOutputStream
import java.io.File

plugins {
    alias(sjy.plugins.buildlogic.lib)
    alias(sjy.plugins.buildlogic.detekt)
}

android {
    namespace = "sjy.sample.cat.fact.ffi"
    defaultConfig {

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Configure source sets to include generated Kotlin bindings
    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin", "build/generated/uniffi/kotlin")
        }
    }
}

dependencies {
    implementation(sjy.androidx.core.ktx)
    implementation(dependencyNotation = sjy.jna) {
        artifact {
            type = "aar"
        }
    }
    implementation(sjy.rustls)
    
    // Coroutines for async wrapper
    implementation(sjy.coroutines.core)
    implementation(sjy.coroutines.android)
}

// Rust build configuration
val rustProjectDir = file("../domain")
val rustTargetDir = file("../domain/target")
val cargoDir = File(System.getProperty("user.home"), ".cargo/bin")
val cargoBinary = if (File(cargoDir, "cargo").exists()) File(cargoDir, "cargo").absolutePath else "cargo"
val rustupBinary = if (File(cargoDir, "rustup").exists()) File(cargoDir, "rustup").absolutePath else "rustup"
val uniffiBindgenBinary = if (File(cargoDir, "uniffi-bindgen").exists()) {
    File(cargoDir, "uniffi-bindgen").absolutePath
} else {
    "uniffi-bindgen"
}

// Map Android ABIs to Rust targets
val rustTargets = mapOf(
    "arm64-v8a" to "aarch64-linux-android",
    "armeabi-v7a" to "armv7-linux-androideabi",
    "x86" to "i686-linux-android",
    "x86_64" to "x86_64-linux-android"
)

// Task to check Rust toolchain
tasks.register<Exec>("checkRustToolchain") {
    group = "rust"
    description = "Verify Rust toolchain is installed"

    environment("PATH", "${cargoDir.absolutePath}${File.pathSeparator}${System.getenv("PATH") ?: ""}")
    commandLine(cargoBinary, "--version")

    doFirst {
        println("🦀 Checking Rust toolchain...")
    }

    doLast {
        println("✅ Rust toolchain is available")
    }
}

// Task to install Rust targets
tasks.register<Exec>("installRustTargets") {
    group = "rust"
    description = "Install required Rust targets for Android"

    dependsOn("checkRustToolchain")

    workingDir = rustProjectDir
    environment("PATH", "${cargoDir.absolutePath}${File.pathSeparator}${System.getenv("PATH") ?: ""}")

    commandLine("sh", "-c", rustTargets.values.joinToString(" && ") { target ->
        "$rustupBinary target add $target"
    })

    doFirst {
        println("📦 Installing Rust targets for Android...")
    }
}

// Task to build Rust library for all Android targets
tasks.register<Exec>("buildRustLibrary") {
    group = "rust"
    description = "Build Rust library for all Android ABIs"

    dependsOn("installRustTargets")

    workingDir = rustProjectDir

    // Check if ANDROID_NDK_HOME is set
    val ndkHome = System.getenv("ANDROID_NDK_HOME")
        ?: project.findProperty("ndk.dir")?.toString()
        ?: throw GradleException("ANDROID_NDK_HOME not set. Please set it in local.properties or environment")

    environment("ANDROID_NDK_HOME", ndkHome)

    // Add NDK toolchain bin directory to PATH for the compiler/linker
    val osName = System.getProperty("os.name").lowercase()
    val hostPlatform = when {
        osName.contains("mac") -> "darwin-x86_64"
        osName.contains("win") -> "windows-x86_64"
        else -> "linux-x86_64"
    }
    val ndkBinDir = file("$ndkHome/toolchains/llvm/prebuilt/$hostPlatform/bin")
    val currentPath = System.getenv("PATH") ?: ""
    environment("PATH", "${cargoDir.absolutePath}${File.pathSeparator}${ndkBinDir.absolutePath}${File.pathSeparator}$currentPath")

    // Configure explicit C compilers for cc-rs cross-compilation
    environment("CC_aarch64_linux_android", "aarch64-linux-android24-clang")
    environment("CC_armv7_linux_androideabi", "armv7a-linux-androideabi24-clang")
    environment("CC_i686_linux_android", "i686-linux-android24-clang")
    environment("CC_x86_64_linux_android", "x86_64-linux-android24-clang")

    commandLine("sh", "-c", rustTargets.values.joinToString(" && ") { target ->
        "$cargoBinary build --release --target $target -p catfact-ffi"
    })

    doFirst {
        println("🔨 Building Rust library for Android...")
        println("   NDK: $ndkHome")
    }

    doLast {
        println("✅ Rust library built successfully")
    }
}

// Task to copy native libraries to jniLibs
tasks.register<Copy>("copyRustLibraries") {
    group = "rust"
    description = "Copy Rust libraries to jniLibs directory"

    dependsOn("buildRustLibrary")

    rustTargets.forEach { (abi, target) ->
        from("$rustTargetDir/$target/release/libcatfact.so") {
            rename { "libuniffi_catfact.so" }
            into(abi)
        }
    }

    into("src/main/jniLibs")

    doFirst {
        println("📦 Copying native libraries to jniLibs...")
    }

    doLast {
        println("✅ Native libraries copied")
    }
}

// Task to install uniffi-bindgen if not present (now handled dynamically by cargo run in generateKotlinBindings)
tasks.register("installUniffiBindgen") {
    group = "rust"
    description = "Install uniffi-bindgen CLI tool (legacy stub)"

    dependsOn("checkRustToolchain")

    doLast {
        println("✅ Using workspace uniffi-bindgen via cargo run")
    }
}

// Task to generate Kotlin bindings
tasks.register<Exec>("generateKotlinBindings") {
    group = "rust"
    description = "Generate Kotlin bindings from UniFFI definition"

    dependsOn("installUniffiBindgen")

    val udlFile = file("../domain/bindings/catfact-ffi/src/catfact.udl")
    val outputDir = file("build/generated/uniffi/kotlin")

    workingDir = rustProjectDir
    environment("PATH", "${cargoDir.absolutePath}${File.pathSeparator}${System.getenv("PATH") ?: ""}")

    commandLine(
        cargoBinary, "run",
        "-p", "catfact-ffi",
        "--features=uniffi/cli",
        "--bin", "uniffi-bindgen",
        "--",
        "generate",
        udlFile.absolutePath,
        "--language", "kotlin",
        "--out-dir", outputDir.absolutePath
    )

    inputs.file(udlFile)
    outputs.dir(outputDir)

    doFirst {
        println("🔧 Generating Kotlin bindings...")
        outputDir.mkdirs()
    }

    doLast {
        println("✅ Kotlin bindings generated at: $outputDir")
    }
}

// Task to build everything
tasks.register("buildRustFFI") {
    group = "rust"
    description = "Build Rust library and generate Kotlin bindings"

    dependsOn("copyRustLibraries", "generateKotlinBindings")

    doLast {
        println("✅ Rust FFI integration complete!")
    }
}

// Hook into Android build process
tasks.named("preBuild") {
    dependsOn("buildRustFFI")
}

// Clean task for Rust artifacts
tasks.register<Delete>("cleanRust") {
    group = "rust"
    description = "Clean Rust build artifacts"

    delete(
        file("src/main/jniLibs"),
        file("build/generated/uniffi")
    )

    doLast {
        val userHome = System.getProperty("user.home")
        val cargoDirLocal = File(userHome, ".cargo/bin")
        val cargoBinaryLocal = if (File(cargoDirLocal, "cargo").exists()) File(cargoDirLocal, "cargo").absolutePath else "cargo"
        val rustProjectDirLocal = File(System.getProperty("user.dir"), "domain")
        try {
            val pb = ProcessBuilder(cargoBinaryLocal, "clean")
                .directory(rustProjectDirLocal)
                .inheritIO()
            pb.environment()["PATH"] = "${cargoDirLocal.absolutePath}${File.pathSeparator}${System.getenv("PATH") ?: ""}"
            pb.start().waitFor()
        } catch (e: Exception) {
            println("Warning: failed to run cargo clean: ${e.message}")
        }
        println("✅ Rust artifacts cleaned")
    }
}

tasks.named("clean") {
    dependsOn("cleanRust")
}

// Ensure all Kotlin compilation and KSP tasks depend on generating the bindings
tasks.configureEach {
    if ((name.contains("compile") && name.contains("Kotlin")) || name.contains("ksp")) {
        dependsOn("generateKotlinBindings")
    }
}
