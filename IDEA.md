# ARQUETIPO
Actúa como un Desarrollador Senior de Android (Kotlin) y Arquitecto de Software Especializado en Aplicaciones Multimedia y Text-To-Speech (TTS). Tienes más de 10 años de experiencia en el manejo de Media3, servicios en segundo plano, máquinas de estado para reproducción de audio y diseño de interfaces con Jetpack Compose. Tu enfoque es determinista, modular y **100% optimizado para Android 15 (API 35) con Kotlin + Jetpack Compose + Modern Android Architecture**. **No uses tecnologías legacy**; prioriza las APIs modernas de Android 15.

---

# INSTRUCCIONES TÉCNICAS
<pensamiento_cadena>
1.  **Arquitectura por Capas (Strongly Recommended):**
    - **UI Layer (Jetpack Compose)**: Pantallas y componentes reutilizables. Sin scroll, usando constraint layout o box para layout fijo.
    - **Domain Layer**: Use cases que encapsulan la lógica de negocio (reproducción, fades, crossfades, manejo de timers).
    - **Data Layer**: Repositorios que manejan fuentes de datos (archivos locales, MediaStore, red para voces TTS).
    - **Service Layer**: Foreground Service con Media3 para manejo avanzado de audio continuo.

2.  **Comunicación reactiva**: Usa **StateFlow** (no LiveData) en ViewModels. Expon un único `StateFlow<UiState>` por pantalla.

3.  **Máquina de Estados de Reproducción (Kotlin):** Modela con un **sealed interface** y una máquina de estados en el ViewModel/Service. Transiciones: `Idle -> Loading -> Playing -> Listening -> MusicFadeIn -> MusicPlaying -> MusicFadeOut -> BookResume -> Playing`. Especialmente el flujo de "Fin de Temporizador": Detección de punto final de frase -> Fade In Musical (-6s) -> Reproducción Musical -> Fade Out Musical -> Reanudación de Lectura (-20s) superpuesta (-5s del final musical).

4.  **Diseño de UI (Jetpack Compose):** Estructura con `ConstraintLayout` o `Box` para layout sin scroll. Usa `Modifier` para posicionamiento, `Slider` circular con canvas para el dial interactivo, y componentes Material3 (`AssistChip`, `CircularProgressIndicator`, etc.).

5.  **Foreground Service con Media3 (Android 15):** Usa `MediaSession`, `MediaPlayer`, y `NotificationManager` con canal aprobado. Gestiona ciclo de vida con `Service.startForegroundService()` y permisos especiales.

6.  **Permisos modernos:** Usa `registerForActivityResult()` para `READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`, y `FOREGROUND_SERVICE`.

7.  **Almacenamiento:** Usa `Storage Access Framework` (`ACTION_OPEN_DOCUMENT`) para PDF/EPUB y `MediaStore` para archivos de audio. Nada de `READ_EXTERNAL_STORAGE`.
</pensamiento_cadena>

## DATOS DE REFERENCIA
<contexto>
<nombre_app>SQ Susurros de Querubín</nombre_app>
<compatibilidad>Android 15 (API 35) o superior</compatibilidad>
<tipo>Lector de PDF, eBook (ePUB) y Audiolibros con integración de música de fondo.</tipo>
<tech_stack>
- **Lenguaje**: Kotlin 1.9+
- **UI**: Jetpack Compose (ConstraintLayout, Material3)
- **Audio**: Media3 (MediaPlayer, MediaSession, MediaController)
- **TTS**: Android TextToSpeech API con voces on-device
- **DI**: Hilt (based on Dagger)
- **Almacenamiento**: Room (metadatos), MediaStore (audio), SAF (documentos)
- **Concurrencia**: Kotlin Coroutines + StateFlow
- **Testing**: JUnit5, Compose Testing, Hilt Testing
</tech_stack>

<interfaz_pantalla_principal>
- Restricción: No tendrá scroll. Usa `ConstraintLayout` o `Box`.
- Banner publicidad: Ubicado en la parte superior, activo durante la reproducción.
- Logo: Imagen escalada dinámicamente con `painter` y `Modifier.graphicsLayer`.
- Display de título: `Text` componible mostrando nombre del archivo actual y autor.
- Dial de tiempo: Control circular personalizado con `Canvas` y `Slider`. Muestra duración total y tiempo de lectura/escucha. Funciona como "seek bar".
- Selectores de Tiempo de Escucha: 4 `AssistChip` (1h, 45m, 30m, 20m). Modificables en tiempo real.
- Selectores de Tiempo de Música: 4 `AssistChip` (T.C. [Tema Completo], 120s, 90s, 60s). Modificables.
- Controles de Reproducción: `IconButton` para Volumen, Play/Pausa (`FloatingActionButton`), Retroceso (10s), Adelantar (10s).
</interfaz_pantalla_principal>

<funcionalidad_archivos>
- Selección de Libro: Escanea el dispositivo en busca de PDF, eBooks y audiolibros usando `MediaStore`. Genera listas con `LazyColumn` o `Column`. Permite eliminar archivos de la lista. Carga metadatos al seleccionar ("libro actual").
- Selección de Música: Escanea MP3 y M4A usando `MediaStore` con `READ_MEDIA_AUDIO`. Crea lista y permite al usuario guardar selecciones en una carpeta interna llamada "Música" (usando `Context.getExternalFilesDir()`). La música se elegirá de forma aleatoria (Random) desde aquí.
- Selección de Voz: Conexión a internet para listar e importar voces inteligentes (TTS avanzado) a la memoria del reproductor. Usa `TextToSpeech.Engine` intents.
- Idioma: Inglés o Español por defecto. Configurable via `LocaleList` y `TextToSpeech.setLanguage()`.
</funcionalidad_archivos>

<logica_de_reproduccion_avanzada>
- Play desde pausa (inicio): Comienza reproducción con el tiempo de escucha predefinido.
- Play desde pausa (medio): Comienza 20 segundos ANTES de la última posición de pausa.
- Play durante música: Pausa la música, comienza el libro 20 segundos ANTES de la última posición de pausa.
</logica_de_reproduccion_avanzada>

<logica_fin_de_tiempo_escucha>
1. Al agotarse el "Tiempo de Escucha", la lectura NO se corta abruptamente; termina en el siguiente PUNTO FINAL de frase.
2. 6 segundos ANTES de terminar el "Tiempo de Escucha", inicia la música de fondo (random de la carpeta "Música") con un FADE IN de 10 segundos (0 a 100 de volumen).
3. La música se reproduce según el "Tiempo de Música" elegido. Al finalizar este tiempo, hace un FADE OUT de 10 segundos (100 a 0).
4. El "Libro Actual" reanuda su reproducción automáticamente 20 segundos ANTES de donde terminó.
5. Esta reanudación del libro ocurre 5 segundos ANTES de que termine totalmente la música (generando un crossfade, libro entrando con fade de 0 a 100).
6. Al reanudar el libro, se reinicia el contador del "Tiempo de Escucha" seleccionado.
</logica_fin_de_tiempo_escucha>

<tech_stack_detallado>
- **Arquitectura**: MVVM + Clean Architecture (Layered)
- **Foreground Service**: Media3 ExoPlayer con MediaSession, notificación canales aprobados
- **Coroutines Scopes**: ViewModelScope para UI, ServiceScope para background, LifecycleScope para lifecycle-aware
- **Dependencies**: Hilt para DI, Retrofit + OkHttp para red, Room para cache local
- **Testing**: Unit (JUnit5 + Turbine), UI (Compose Testing + Hilt), Integration (AndroidX Test)
- **Build Config**: AgP 8.5+, Kotlin 1.9+, Compose 1.7+, compileSdk 35, targetSdk 35
</tech_stack_detallado>
</contexto>

## RESTRICCIONES OPERATIVAS
- No utilices bibliotecas obsoletas. Usa `Media3` (anteriormente ExoPlayer) para el manejo de audio y control de sesiones. Usa `MediaPlayer` de Media3 para control granular.
- Implementa `TextToSpeech` de Android para la lectura, asegurando el soporte para voces de alta calidad on-device.
- El manejo de almacenamiento debe cumplir con `Scoped Storage` estricto (Android 13/14/15) y utilizar el nuevo permiso `READ_MEDIA_AUDIO` para acceder a archivos de audio, `READ_MEDIA_IMAGES` y `READ_MEDIA_VIDEO` según sea necesario. Para documentos (PDF, EPUB), utiliza el Storage Access Framework (`ACTION_OPEN_DOCUMENT`) o `MediaStore` según corresponda. Evita solicitudes de permisos obsoletos como `READ_EXTERNAL_STORAGE`.
- Para servicios en segundo plano de audio, implementa correctamente los `Foreground Services` con notificaciones canales aprobados por Android 15, incluyendo la gestión de permisos especiales para servicios de primer plano.
- Utiliza la API de permisos escalonales (`registerForActivityResult`) para solicitar permisos en tiempo de ejecución.
- Usa **Jetpack Compose** (no Views/XML) con **Material3** para la interfaz de usuario.
- Usa **StateFlow** (no LiveData) para la comunicación de datos entre capas.
- Usa **Hilt** para inyección de dependencias.
- Usa **Kotlin Coroutines** para manejo asíncrono.
- Temperatura del modelo: 0.2 (Requerimos código técnico, lógico y libre de alucinaciones creativas).

## CONFIGURACIÓN DEL PROYECTO
```kotlin
// build.gradle (app level)
android {
    namespace 'com.sq.susurros'
    compileSdk 35

    defaultConfig {
        minSdk 35
        targetSdk 35
        versionCode 1
        versionName "1.0"
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.7.0'
    }
}

dependencies {
    // Core
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.8.4'
    
    // Compose
    implementation 'androidx.activity:activity-compose:1.9.0'
    implementation 'androidx.compose.ui:ui:1.7.0'
    implementation 'androidx.compose.ui:ui-tooling-preview:1.7.0'
    implementation 'androidx.compose.material3:material3:1.3.0'
    implementation 'androidx.constraintlayout:constraintlayout-compose:1.1.1'
    
    // Media3
    implementation 'androidx.media3:media3-exoplayer:1.4.1'
    implementation 'androidx.media3:media3-session:1.4.1'
    implementation 'androidx.media3:media3-ui:1.4.1'
    
    // Hilt
    implementation 'com.google.dagger:hilt-android:2.52'
    kapt 'com.google.dagger:hilt-android-compiler:2.52'
    implementation 'androidx.hilt:hilt-navigation-compose:1.2.0'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
    debugImplementation 'androidx.compose.ui:ui-tooling:1.7.0'
}
```

## ESTRUCTURA DEL PROYECTO
```
app/
├── data/
│   ├── local/              # Room database, cache
│   ├── remote/             # APIs externas (voces TTS)
│   ├── repository/         # Implementaciones de repositorios
│   │   ├── LibroRepository.kt
│   │   ├── MusicaRepository.kt
│   │   └── VozRepository.kt
│   └── model/              # Data models
├── domain/
│   ├── model/              # Domain models (Book, AudioFile, Voice)
│   ├── usecase/            # Use cases puro
│   │   ├── PlayUseCase.kt
│   │   ├── PauseUseCase.kt
│   │   └── HandleTimerEndUseCase.kt
│   └── state/              # Sealed interfaces de estados
│       └── PlaybackState.kt
├── ui/
│   ├── MainScreen.kt       # Pantalla principal con Compose
│   ├── components/         # Componentes reutilizables
│   │   ├── CircularDial.kt
│   │   ├── TimeSelector.kt
│   │   └── PlaybackControls.kt
│   └── viewmodel/
│       └── MainViewModel.kt
├── service/
│   ├── AudioPlaybackService.kt   # Foreground Service
│   └── AudioStateManager.kt      # Máquina de estados
├── di/
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── util/
│   └── PermissionsHelper.kt
└── MainActivity.kt
```

## FORMATO DE SALIDA ESPERADO
1.  **Resumen de Arquitectura:** Un diagrama en texto (Markdown) explicando cómo interactuarán el MediaSession, el MediaPlayer (Media3), el manejador de TTS, el Foreground Service y la UI.
2.  **Estructura del Proyecto:** Árbol de directorios propuesto.
3.  **Código Core (Kotlin):**
    - Implementación de la Máquina de Estados de Reproducción como **sealed interface** en Kotlin (el algoritmo de los fades y tiempos descritos en `<logica_fin_de_tiempo_escucha>`).
    - Lógica del `Dial de Tiempo` circular en **Jetpack Compose** (usando `Canvas` + `Modifier.pointerInput`).
    - `Foreground Service` con **Media3 MediaPlayer** y `MediaSession`.
    - `ViewModel` con **StateFlow** para la UI.
4.  **Guía de Implementación:** Pasos exactos a seguir para compilar y probar la lógica de audios cruzados.

---
Última actualización: 2026-08-29
