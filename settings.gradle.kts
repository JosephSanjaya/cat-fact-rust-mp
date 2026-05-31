pluginManagement {
    includeBuild("sjy-build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    val localVerifierRepo: java.io.File? = run {
        val userHome = System.getProperty("user.home")
        val registrySrcDir = java.io.File(userHome, ".cargo/registry/src")
        if (registrySrcDir.exists()) {
            registrySrcDir.listFiles()?.forEach { indexDir ->
                if (indexDir.isDirectory) {
                    val candidateDirs = indexDir.listFiles { _, name -> name.startsWith("rustls-platform-verifier-android-") }
                    candidateDirs?.firstOrNull()?.let { verifierDir ->
                        val mavenDir = java.io.File(verifierDir, "maven")
                        if (mavenDir.exists()) {
                            return@run mavenDir
                        }
                    }
                }
            }
        }
        null
    }

    repositories {
        google()
        mavenCentral()
        localVerifierRepo?.let {
            maven {
                url = it.toURI()
            }
        }
    }
    versionCatalogs {
        create("sjy") {
            from(files("sjy-build-logic/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Cat Fact"
include(":app")
include(":rust-ffi")
