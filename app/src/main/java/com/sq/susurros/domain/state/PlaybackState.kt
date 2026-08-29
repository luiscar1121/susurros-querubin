// app/src/main/java/com/sq/susurros/domain/state/PlaybackState.kt
package com.sq.susurros.domain.state

/**
 * Máquina de estados de reproducción para SQ Susurros de Querubín.
 *
 * Transiciones esperadas:
 * Idle -> Loading -> Listening -> MusicFadeIn -> MusicPlaying
 *   -> MusicFadeOut -> BookResume -> Listening (ciclo reinicia)
 *
 * Flujo de Timer End (lógica de fades):
 * 1. Listening: TTS lee el libro hasta agotarse el Timer de Escucha.
 *    - Al agotarse: la lectura NO corta; termina en el próximo PUNTO FINAL de frase.
 * 2. 6s ANTES del fin del Timer de Escucha -> MusicFadeIn (10s fade 0->100).
 * 3. MusicPlaying: música se reproduce según Tiempo de Música seleccionado.
 * 4. Al finalizar Tiempo de Música -> MusicFadeOut (10s fade 100->0).
 * 5. 5s ANTES de que termine la música (en el fade-out) -> BookResume.
 *    Crossfade simétrico de 5s: libro entra con fade 0->100 mientras
 *    música termina su fade-out 100->0 en los mismos 5s.
 * 6. Al reanudar el libro, se reinicia el contador del Timer de Escucha.
 *
 * @property bookPosition Posición actual en el libro (ms).
 * @property remaining Tiempo restante del Timer de Escucha (ms).
 * @property activeTimer Duración total del Timer de Escucha (ms).
 */
sealed interface PlaybackState {

    /** Estado inicial: nada cargado ni reproduciéndose. */
    data object Idle : PlaybackState

    /** Estado de carga: se está preparando el MediaPlayer/TTS. */
    data object Loading : PlaybackState

    /**
     * Estado activo de lectura: el TTS está reproduciendo el libro.
     * Se monitorea [remaining] y [bookPosition] en tiempo real.
     */
    data class Listening(
        val bookPosition: Long = 0L,
        val remaining: Long = 0L,
        val activeTimer: Long = 0L,
        val isBookPlaying: Boolean = true,
        val bookVolume: Float = 1.0f,
        val musicVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * Fade-in de la música de fondo: se inicia 6s antes del fin del Timer de Escucha.
     * Dura 10 segundos (0 -> 100% volumen).
     */
    data class MusicFadeIn(
        val bookPosition: Long,
        val remaining: Long,
        val activeTimer: Long,
        val musicPosition: Long = 0L,
        val elapsed: Long = 0L,
        val duration: Long = 10_000L, // 10 segundos
        val targetVolume: Float = 1.0f,
        val bookVolume: Float = 1.0f,
        val musicVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * Música de fondo reproduciéndose a volumen completo.
     * [remaining] es el tiempo restante del "Tiempo de Música" seleccionado.
     */
    data class MusicPlaying(
        val bookPosition: Long,
        val musicPosition: Long,
        val remaining: Long,
        val activeTimer: Long,
        val duration: Long,
        val musicVolume: Float = 1.0f,
        val bookVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * Fade-out de la música: inicia cuando se agota el "Tiempo de Música".
     * Dura 10 segundos (100 -> 0% volumen).
     * 5 segundos antes de terminar, se inicia el BookResume (crossfade).
     */
    data class MusicFadeOut(
        val bookPosition: Long,
        val musicPosition: Long,
        val remaining: Long,
        val activeTimer: Long,
        val elapsed: Long = 0L,
        val duration: Long = 10_000L, // 10 segundos
        val startVolume: Float = 1.0f,
        val musicVolume: Float = 1.0f,
        val bookVolume: Float = 0.0f
    ) : PlaybackState

    /**
     * El libro reanuda su lectura en un crossfade simétrico de 5 segundos.
     * El libro comienza 20 segundos ANTES de la última posición de pausa.
     * Entra con fade 0 -> 100 mientras la música termina su fade-out.
     */
    data class BookResume(
        val bookPosition: Long,
        val musicPosition: Long,
        val remaining: Long,
        val activeTimer: Long,
        val crossfadeElapsed: Long = 0L,
        val crossfadeDuration: Long = 5_000L, // 5 segundos simétricos
        val bookVolume: Float = 0.0f,
        val musicVolume: Float = 1.0f
    ) : PlaybackState
}

/**
 * Extensiones de utilidad para la máquina de estados.
 */
val PlaybackState.isPlaying: Boolean
    get() = when (this) {
        is PlaybackState.Listening -> isBookPlaying
        is PlaybackState.MusicFadeIn,
        is PlaybackState.MusicPlaying,
        is PlaybackState.MusicFadeOut,
        is PlaybackState.BookResume -> true
        is PlaybackState.Idle,
        is PlaybackState.Loading -> false
    }

val PlaybackState.bookVolume: Float
    get() = when (this) {
        is PlaybackState.Idle -> 0f
        is PlaybackState.Loading -> 0f
        is PlaybackState.Listening -> bookVolume
        is PlaybackState.MusicFadeIn -> bookVolume
        is PlaybackState.MusicPlaying -> bookVolume
        is PlaybackState.MusicFadeOut -> bookVolume
        is PlaybackState.BookResume -> bookVolume
    }

val PlaybackState.musicVolume: Float
    get() = when (this) {
        is PlaybackState.Idle -> 0f
        is PlaybackState.Loading -> 0f
        is PlaybackState.Listening -> musicVolume
        is PlaybackState.MusicFadeIn -> musicVolume
        is PlaybackState.MusicPlaying -> musicVolume
        is PlaybackState.MusicFadeOut -> musicVolume
        is PlaybackState.BookResume -> musicVolume
    }
