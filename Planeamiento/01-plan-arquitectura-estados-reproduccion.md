# SQ Susurros de Querubín — Plan de Implementación (Arquitectura & Máquina de Estados)

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Establecer la arquitectura base y la máquina de estados de reproducción para "SQ Susurros de Querubín", una app Android 15 (API 35) de lectura de PDF/ePub/audiolibros con TTS on-device y música de fondo con fades y crossfades.

**Arquitectura:** MVVM + Clean Architecture (Layerada) con Foreground Service Media3, StateFlow reactivo, Hilt DI, y una máquina de estados sellada que orquesta el flujo de fades de música y reanudación de libros.

**Tech Stack:** Kotlin 1.9+, Jetpack Compose 1.7+/Material3, Media3 (ExoPlayer), Android TextToSpeech, Hilt, Room, Coroutines + StateFlow, Scoped Storage (SAF/MediaStore), Android 15 permisos escalonales.

---

## 1. Resumen de Arquitectura

```
┌─────────────────────────────────────────────────┐
│  PRESENTACIÓN (Jetpack Compose)                  │
│  ┌──────────────┐   ┌──────────────────┐         │
│  │ MainScreen   │   │ MainViewModel    │         │
│  │ - Box layout │───│ - StateFlow      │         │
│  │ - Canvas dial│   │ - PlaybackState  │         │
│  │ - AssistChips│   │ - UseCases       │         │
│  └──────────────┘   └──────────┬───────┘         │
└──────────────────────────────┬───┘                 │
                               ▼                     │
┌────────────────────────── DOMINIO ─────────────────┐
│  Use Cases:                                      │
│  • StartPlaybackUseCase (Idle→Loading→Playing)   │
│  • PausePlaybackUseCase                          │
│  • SeekUseCase                                   │
│  • HandleTimerEndUseCase (fade orchestration)    │
│  • ResumeBookUseCase (crossfade timing)          │
└───────────────────────────┬──────────────────────┘
                            ▼
┌────────────────────── DATOS ──────────────────────┐
│  LibroRepository   — MediaStore/SAF (PDF, ePub)  │
│  MusicaRepository  — MediaStore (MP3/M4A)        │
│  VozRepository     — TextToSpeech.Engine intents │
│  (Cache local con Room para metadatos)            │
└───────────────────────────┬──────────────────────┘
                            ▼
┌────────────────── SERVICIOS ──────────────────────┐
│  AudioPlaybackService (Foreground)                │
│  • MediaPlayer (Media3) — stream de libro/TTS    │
│  • AudioStateManager — máquina de estados          │
│  • MediaSession — conexión con MediaController    │
│  • Timer + Handler — lógica precisa de fades    │
└───────────────────────────────────────────────────┘
```

El `AudioPlaybackService` es el corazón del timing de fades. El `MainViewModel` expone estado reactivo vía un único `StateFlow<UiState>` y delega a use cases. El `AudioStateManager` mantiene la máquina de estados con sello (`sealed interface`) y los contadores de tiempo precisos.

---

## 2. Estructura del Proyecto

```
app/
├── data/
│   ├── local/              # Room database (AppDatabase.kt, DAOs)
│   ├── remote/             # API de voces TTS (VoiceApi.kt)
│   ├── repository/
│   │   ├── LibroRepository.kt
│   │   ├── MusicaRepository.kt
│   │   └── VozRepository.kt
│   └── model/              # Data models (BookData, MusicData, VoiceData)
├── domain/
│   ├── model/              # Domain models (Book, AudioFile, Voice, TimerConfig)
│   ├── usecase/
│   │   ├── StartPlaybackUseCase.kt
│   │   ├── PausePlaybackUseCase.kt
│   │   ├── SeekUseCase.kt
│   │   ├── HandleTimerEndUseCase.kt
│   │   └── ResumeBookUseCase.kt
│   └── state/
│       └── PlaybackState.kt        # sealed interface
├── ui/
│   ├── MainScreen.kt
│   ├── components/
│   │   ├── CircularDial.kt
│   │   ├── TimeSelector.kt
│   │   └── PlaybackControls.kt
│   └── viewmodel/
│       └── MainViewModel.kt
├── service/
│   ├── AudioPlaybackService.kt
│   └── AudioStateManager.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── util/
│   ├── PermissionsHelper.kt
│   └── TimerHelper.kt
├── MainActivity.kt
└── App.kt                    # Application con Hilt
```

---

## 3. Código Core

### 3.1 Máquina de Estados de Reproducción (`PlaybackState.kt`)

```kotlin
// domain/state/PlaybackState.kt
sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Loading : PlaybackState
    data class Listening(
        val bookPosition: Long,
        val remaining: Long,
        val activeTimer: Long,
        val isBookPlaying: Boolean
    ) : PlaybackState
    data class MusicFadeIn(
        val musicPosition: Long,
        val elapsed: Long,
        val duration: Long = 10_000L, // 10 segundos
        val targetVolume: Float = 1.0f
    ) : PlaybackState
    data class MusicPlaying(
        val musicPosition: Long,
        val remaining: Long,
        val duration: Long,
        val musicVolume: Float = 1.0f
    ) : PlaybackState
    data class MusicFadeOut(
        val musicPosition: Long,
        val elapsed: Long,
        val duration: Long = 10_000L,
        val startVolume: Float = 1.0f
    ) : PlaybackState
    data class BookResume(
        val bookPosition: Long,
        val crossfadeElapsed: Long,
        val crossfadeDuration: Long = 5_000L
    ) : PlaybackState
}
```

Transiciones esperadas:
```
Idle → Loading → Playing/Listening → MusicFadeIn → MusicPlaying → MusicFadeOut → BookResume → Listening
```

### 3.2 Dial de Tiempo Circular (Compose)

```kotlin
// ui/components/CircularDial.kt
@Composable
fun CircularDial(
    currentTime: String,          // "08:42"
    remainingTime: String,        // "RESTA 03:18"
    progress: Float,              // 0.0..1.0 del progreso del Timer de Escucha
    onDrag: (Float) -> Unit       // callback al arrastrar
) {
    val strokeWidth = 8.dp
    val dialSize = 220.dp

    Box(
        modifier = Modifier
            .size(dialSize)
            .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
            .border(4.dp, MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val centerX = size.center.x
                    val centerY = size.center.y
                    val angle = atan2(
                        change.position.y - centerY,
                        change.position.x - centerX
                    )
                    val adjustedAngle = if (angle < 0) angle + 2 * PI else angle
                    val newProgress = (adjustedAngle / (2 * PI)).toFloat()
                    onDrag(newProgress)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Track de fondo
            drawArc(
                color = Color(0x33FFFFFF),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Progreso activo
            drawArc(
                color = MaterialTheme.colorScheme.primary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentTime,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = remainingTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}
```

### 3.3 Foreground Service con Media3

```kotlin
// service/AudioPlaybackService.kt
@AndroidEntryPoint
class AudioPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSession
    private lateinit var stateManager: AudioStateManager
    private var musicPlayer: MediaPlayer? = null // para música de fondo

    override fun onCreate() {
        super.onCreate()
        stateManager = AudioStateManager()
        initMediaSession()
        initPlayers()
    }

    private fun initPlayers() {
        mediaPlayer = MediaPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build())
            .build()

        musicPlayer = MediaPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build())
            .build()
    }

    private fun initMediaSession() {
        mediaSession = MediaSession.Builder(this, surface)
            .setName("SQ Susurros")
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaSession.release()
        mediaPlayer?.release()
        musicPlayer?.release()
        super.onDestroy()
    }

    private fun createNotification(): Notification { /* detalles en task */ }
}
```

### 3.4 ViewModel con StateFlow

```kotlin
// ui/viewmodel/MainViewModel.kt
@HiltViewModel
class MainViewModel @Inject constructor(
    private val startPlayback: StartPlaybackUseCase,
    private val pausePlayback: PausePlaybackUseCase,
    private val handleTimerEnd: HandleTimerEndUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onPlayPause() {
        viewModelScope.launch {
            if (_uiState.value.isPlaying) {
                pausePlayback()
                _uiState.update { it.copy(isPlaying = false, playbackState = PlaybackState.Idle) }
            } else {
                startPlayback()
                _uiState.update { it.copy(isPlaying = true, playbackState = PlaybackState.Listening(...)) }
            }
        }
    }
}
```

---

## 4. Guía de Implementación (Tareas)

### Task 1: Setup del proyecto Android (build.gradle)

**Objective:** Crear la estructura base del proyecto con AGP 8.5, Kotlin 1.9, Compose 1.7, Media3, Hilt y configuración de permisos para Android 15.

**Files:**
- Create: `app/build.gradle.kts` (o `.kt` según convención)
- Create: `build.gradle.kts` (project level)

**Steps:**
1. Crear el proyecto con `namespace = "com.sq.susurros"`, `compileSdk = 35`, `minSdk = 35`, `targetSdk = 35`.
2. Agregar dependencias:
   ```kotlin
   implementation("androidx.media3:media3-exoplayer:1.4.1")
   implementation("androidx.media3:media3-session:1.4.1")
   implementation("androidx.compose.material3:material3:1.3.0")
   implementation("com.google.dagger:hilt-android:2.52")
   kapt("com.google.dagger:hilt-android-compiler:2.52")
   ```
3. Crear `App.kt` con `@HiltAndroidApp`.

### Task 2: Máquina de Estados (PlaybackState)

**Objective:** Definir la `sealed interface PlaybackState` con todos los estados y sus datos.

**File:** `domain/state/PlaybackState.kt`

**Steps:**
1. Crear `sealed interface PlaybackState`.
2. Implementar estados: `Idle`, `Listening`, `MusicFadeIn`, `MusicPlaying`, `MusicFadeOut`, `BookResume`.
3. Cada estado con datos necesarios para timing.
4. Escribir test unitario verificando que los estados sean mutuamente excluyentes.

### Task 3: AudioStateManager (servicio de estados)

**Objective:** Implementar `AudioStateManager` dentro del `Foreground Service` que orquesta los fades.

**File:** `service/AudioStateManager.kt`

**Steps:**
1. Crear clase `AudioStateManager` con métodos de transición.
2. Implementar lógica de timers:
   - 6s antes de fin del Timer de Escucha → iniciar fade-in de música (10s).
   - Al final del Tiempo de Música → fade-out (10s).
   - 5s antes de fin de la música → iniciar reanudación del libro con crossfade (5s).
3. Escribir tests con Turbine verificando transiciones de estado.

### Task 4: Foreground Service con Media3

**Objective:** Implementar `AudioPlaybackService` con `MediaPlayer`, `MediaSession` y notificación.

**File:** `service/AudioPlaybackService.kt`

**Steps:**
1. Crear servicio con `@AndroidEntryPoint`.
2. Inicializar `MediaPlayer` para libro/TTS y segundo `MediaPlayer` para música.
3. Configurar `MediaSession` conectado a `MediaController`.
4. Notificación aprobada para Android 15 con canal de notificación.
5. Manejar `startForegroundService()` y permisos.

### Task 5: ViewModel con StateFlow reactivo

**Objective:** Implementar `MainViewModel` con un único `StateFlow<MainUiState>`.

**File:** `ui/viewmodel/MainViewModel.kt`

**Steps:**
1. Crear `MainUiState` data class con: posición del libro, tiempo restante, volumen, estado de reproducción, estado de la máquina de estados.
2. Inyectar use cases con Hilt.
3. Exponer `StateFlow<MainUiState>` y métodos para interactuar (play/pause, seek, ajustar timer).
4. Tests con Turbine verificando emisiones de estado.

### Task 6: Dial de Tiempo Circular (Compose)

**Objective:** Implementar el control circular interactivo con `Canvas` y `pointerInput`.

**File:** `ui/components/CircularDial.kt`

**Steps:**
1. Crear composable con `Box`, `Canvas` para el arco de progreso.
2. Implementar detección de drag con `detectDragGestures`.
3. Calcular ángulo → progreso → callback.
4. Preview de Compose con estado simulado.

### Task 7: Selectores de Tiempo (AssistChips)

**Objective:** Implementar los 8 chips (4 para Tiempo de Escucha, 4 para Tiempo de Música).

**File:** `ui/components/TimeSelector.kt`

**Steps:**
1. Crear composable `TimeSelector` con grid de 4 chips.
2. Chips: `1h, 45m, 30m, 20m` y `T.C., 120s, 90s, 60s`.
3. Estado visual: seleccionado vs no seleccionado (colores del DESIGN.md).
4. Callback de selección.

### Task 8: Controles de Reproducción (Compose)

**Objective:** Implementar los controles: retroceso 10s, play/pause, adelante 10s, volumen.

**File:** `ui/components/PlaybackControls.kt`

**Steps:**
1. Crear `FloatingActionButton` para play/pause con icono dinámico.
2. `IconButton` para retroceso/adelanto (10s).
3. Slider horizontal de volumen.
4. Conectar callbacks al ViewModel.

### Task 9: MainScreen (composición completa)

**Objective:** Componer la pantalla principal sin scroll usando `ConstraintLayout`.

**File:** `ui/MainScreen.kt`

**Steps:**
1. Layout con `ConstraintLayout`: banner, logo+título, dial, chips, controles.
2. Observar `StateFlow<MainUiState>` con `collectAsState()`.
3. Binding de datos: posición libro, tiempo restante, volumen.

### Task 10: Repositorios (Data Layer)

**Objective:** Implementar repositorios para libros, música y voces.

**Files:**
- `data/repository/LibroRepository.kt`
- `data/repository/MusicaRepository.kt`
- `data/repository/VozRepository.kt`

**Steps:**
1. `LibroRepository`: escanear PDF/EPUB via `MediaStore` y `ACTION_OPEN_DOCUMENT`.
2. `MusicaRepository`: escanear MP3/M4A, guardar en carpeta interna "Música".
3. `VozRepository`: listar voces TTS, descargar voces inteligentes on-device.
4. Cache de metadatos con Room.

---

## 5. Tests / Validación

| Task | Test | Herramienta |
|------|------|-------------|
| 2 | Verificar estados mutuamente excluyentes | JUnit5 |
| 3 | Verificar transiciones de timeline de fades | Turbine + Coroutines |
| 5 | Verificar emisiones StateFlow | Turbine |
| 6 | Comprobar interacción de drag del dial | Compose Testing |
| 10 | Escaneo de archivos via MediaStore | AndroidX Test (Instrumentation) |

---

## 6. Riesgos, Tradeoffs y Preguntas Abiertos

1. ✅ **Precisión de timers:** Aceptado usar la versión simple. Coroutines con `delay()` + ajuste dinámico del progreso (no `ValueAnimator`). El drift es aceptable para UX de audiolibros.
2. ✅ **TTS + MusicPlayer coexistiendo:** El `AudioStateManager` manejará manualmente los volúmenes de ambos `MediaPlayer` para un crossfade limpio. Sin ducking automático del sistema.
3. ✅ **Scoped Storage + `READ_MEDIA_AUDIO`:** Flujo reactivo con `registerForActivityResult()` directamente en `MainActivity`, con callback al ViewModel via StateFlow.
4. ✅ **Notificación del Foreground Service:** La notificación incluirá acciones de play/pause/seek para interacción directa. Channel con `IMPORTANCE_LOW`.
5. ✅ **Crossfade (5s superpuesto):** Simétrico — 5s de solapamiento completo. El libro reanuda 5s antes del fin de la música con fade-in (0→100) mientras la música completa su fade-out (100→0) en los mismos 5s.

---

*Plan guardado: `Planeamiento/01-plan-arquitectura-estados-reproduccion.md` | Fecha: 2026-08-29*
