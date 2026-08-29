# Setup del Proyecto Android para SQ Susurros de Querubín

> **Task 1 del plan de implementación**

## Información de contexto

- **App:** SQ Susurros de Querubín (`com.sq.susurros`)
- **Target:** Android 15 (API 35)
- **Lenguaje:** Kotlin 1.9+
- **UI:** Jetpack Compose 1.7+/Material3
- **Audio:** Media3 (ExoPlayer)
- **DI:** Hilt

## Entorno actual

- **Plataforma host:** Windows 10
- **Shell:** bash (git-bash / MSYS)
- **Working dir:** `D:\Susurros Querubin`
- **Python:** `python` 3.11.16 (sin pip), `python3` falta
- **Hermes profile:** default

> **Nota importante:** Este proyecto requiere el SDK de Android y Gradle para compilar. Verificaremos si están disponibles antes de generar los archivos de proyecto. Si no, generaremos la estructura de archivos base que puede importarse a Android Studio posteriormente.

## Instrucciones

1. Verificar si `java` y `gradle` están disponibles en el sistema.
2. Si Java está disponible, clonar/crear el esqueleto del proyecto con los archivos de Gradle y manifiesto base.
3. Si no, crear los archivos de estructura base (build.gradle.kts, AndroidManifest.xml, App.kt, SettingsActivity) preparados para ser importados en Android Studio.

## Files a crear

- `build.gradle.kts` (project level)
- `gradle.properties`
- `settings.gradle.kts`
- `app/build.gradle.kts` (module level)
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/sq/susurros/App.kt`
- `app/src/main/java/com/sq/susurros/MainActivity.kt`

## Verificación

- Los archivos de Gradle usan la sintaxis correcta de Kotlin DSL.
- El namespace es `com.sq.susurros`.
- compileSdk/targetSdk/minSdk = 35/35/35.
- Las dependencias de Media3, Compose 1.7 y Hilt están correctamente versionadas.
