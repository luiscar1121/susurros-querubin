// Top-level build.gradle.kts
// SQ Susurros de Querubín — Project-level configuration

plugins {
    alias(libs.plugins.android.application) version "8.5.2" apply false
    alias(libs.plugins.kotlin.android) version "1.9.0" apply false
    alias(libs.plugins.kotlin.kapt) version "1.9.0" apply false
    alias(libs.plugins.hilt.android) version "2.52" apply false
}

// Si no se usa en version catalog, definir versiones aquí
// Versión de Compose 1.7 y Material3 1.3 como especifica el plan
