// app/src/main/java/com/sq/susurros/domain/state/PlaybackState.kt
package com.sq.susurros.domain.state

/**
 * Máquina de estados de reproducción para SQ Susurros de Querubín (Versión 02).
 *
 * Transiciones:
 * Idle -> Loading -> Playing -> Listening -> MusicFadeIn -> MusicPlaying 
 *   -> MusicFadeOut -> BookResume -> Playing (ciclo reinicia)
 *
 * @property bookPosition Posición actual en el libro (ms).
 * @property remaining Tiempo restante del Timer de Escucha (ms).
 * @property bookVolume Volumen del libro (0.0 a 1.0).
 * @property musicVolume Volumen de la música (0.0 a 1.0).
 */
sealed interface PlaybackState {
    val bookPosition: Long
    val remaining: Long
    val bookVolume: Float
    val musicVolume: Float

    /** Estado inicial: nada cargado ni reproduciéndose. */
    data object Idle : PlaybackState {
        override val bookPosition: Long = 0L
        override val remaining: Long = 0L
        override val bookVolume: Float = 0f
        override val musicVolume: Float = 0f
    }

    /** Estado de carga: se está preparando el motor TTS o cargando archivos. */
    data object Loading : PlaybackState {
        override val bookPosition: Long = 0L
        override val remaining: Long = 0L
        override val bookVolume: Float = 0f
        override val musicVolume: Float = 0f
    }

    /** Reproducción normal del libro sin temporizador activo. */
    data class Playing(
        override val bookPosition: Long = 0L,
        override val bookVolume: Float = 1.0f,
        override val musicVolume: Float = 0.0f
    ) : PlaybackState {
        override val remaining: Long = 0L
    }

    /** 
     * Estado activo de lectura con temporizador: el TTS está reproduciendo el libro.
     * Se monitorea [remaining] en tiempo real.
     */
    data class Listening(
        override val bookPosition: Long = 0L,
        override val remaining: Long = 0L,
        val activeTimer: Long = 0L,
        val isBookPlaying: Boolean = true,
        override val bookVolume: Float = 1.0f,
        override val musicVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * Fade-in de la música de fondo: se inicia 6s antes del fin del Timer de Escucha.
     * Dura 10 segundos (0 -> 100% volumen).
     */
    data class MusicFadeIn(
        override val bookPosition: Long,
        override val remaining: Long,
        val activeTimer: Long,
        val musicPosition: Long = 0L,
        val elapsed: Long = 0L,
        val duration: Long = 10_000L,
        val targetVolume: Float = 1.0f,
        override val bookVolume: Float = 1.0f,
        override val musicVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * Música de fondo reproduciéndose a volumen completo.
     * [remaining] es el tiempo restante del "Tiempo de Música" seleccionado.
     */
    data class MusicPlaying(
        override val bookPosition: Long,
        val musicPosition: Long,
        override val remaining: Long,
        val activeTimer: Long,
        val duration: Long,
        override val musicVolume: Float = 1.0f,
        override val bookVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * Fade-out de la música: inicia cuando se agota el "Tiempo de Música".
     * Dura 10 segundos (100 -> 0% volumen).
     */
    data class MusicFadeOut(
        override val bookPosition: Long,
        val musicPosition: Long,
        override val remaining: Long,
        val activeTimer: Long,
        val elapsed: Long = 0L,
        val duration: Long = 10_000L,
        val startVolume: Float = 1.0f,
        override val musicVolume: Float = 1.0f,
        override val bookVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * El libro reanuda su lectura en un crossfade simétrico de 5 segundos.
     * El libro comienza 20 segundos ANTES de la última posición de pausa.
     */
    data class BookResume(
        override val bookPosition: Long,
        val musicPosition: Long,
        override val remaining: Long,
        val activeTimer: Long,
        val crossfadeElapsed: Long = 0L,
        val crossfadeDuration: Long = 5_000L,
        override val bookVolume: Float = 0.0f,
        override val musicVolume: Float = 1.0f
    ) : PlaybackState
}

/**
 * Extensiones de utilidad.
 */
val PlaybackState.isPlaying: Boolean
    get() = when (this) {
        is PlaybackState.Playing -> true
        is PlaybackState.Listening -> isBookPlaying
        is PlaybackState.MusicFadeIn,
        is PlaybackState.MusicPlaying,
        is PlaybackState.MusicFadeOut,
        is PlaybackState.BookResume -> true
        is PlaybackState.Idle,
        is PlaybackState.Loading -> false
    }
