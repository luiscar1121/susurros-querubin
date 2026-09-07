// Top-level build.gradle.kts
// SQ Susurros de Querubín — Project-level configuration

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    id("dagger.hilt.android.plugin") version "2.52" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}

// Si no se usa en version catalog, definir versiones aquí
// Versión de Compose 1.7 y Material3 1.3 como especifica el plan