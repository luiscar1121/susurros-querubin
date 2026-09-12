// app/src/main/java/com/sq/susurros/service/AudioStateManager.kt
package com.sq.susurros.service

import com.sq.susurros.domain.state.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

/**
 * AudioStateManager — Máquina de estados de reproducción.
 *
 * Se encarga de orquestar el timing de los fades de música y la
 * reanudación del libro dentro del Foreground Service.
 *
 * Uso:
 *   val manager = AudioStateManager()
 *   manager.startListening(bookPosition = 0, timerMs = 3_600_000L)
 *   // ... ticks periódicos ...
 *   manager.onTick(elapsedMs = 60_000L)
 *
 * @property _state StateFlow reactivo expuesto a la UI via ViewModel.
 */
class AudioStateManager {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /**
     * Inicia la fase de escucha (TTS leyendo el libro).
     *
     * @param timerMs Duración del "Tiempo de Escucha" (1h, 45m, 30m, 20m).
     */
    fun startListening(bookPosition: Long = 0L, timerMs: Long) {
        _state.value = PlaybackState.Listening(
            bookPosition = bookPosition,
            remaining = timerMs,
            activeTimer = timerMs,
            isBookPlaying = true,
            bookVolume = 1.0f,
            musicVolume = 0.0f
        )
    }

    /**
     * Procesa un tick periódico (ej: cada 100ms) y actualiza el estado.
     * Este método contiene TODA la lógica de fades y crossfades.
     *
     * @param elapsedMs Tiempo transcurrido desde el último tick (delta time).
     */
    fun onTick(elapsedMs: Long) {
        val current = _state.value

        when (current) {
            is PlaybackState.Listening -> {
                val remaining = current.remaining - elapsedMs
                val bookPosition = current.bookPosition + elapsedMs

                // 6 segundos ANTES del fin del Timer de Escucha -> iniciar MusicFadeIn
                if (remaining <= 6_000L && remaining > 0) {
                    val overflow = 6_000L - remaining
                    _state.value = PlaybackState.MusicFadeIn(
                        bookPosition = bookPosition,
                        remaining = remaining,
                        activeTimer = current.activeTimer,
                        musicPosition = 0L,
                        elapsed = overflow,
                        duration = 10_000L,
                        targetVolume = 1.0f,
                        bookVolume = 1.0f,
                        musicVolume = (overflow.toFloat() / 10_000f).coerceIn(0f, 1f)
                    )
                }
                // Si ya agotó el timer -> buscar punto final de frase y terminar
                else if (remaining <= 0) {
                    _state.value = PlaybackState.Idle
                } else {
                    _state.value = PlaybackState.Listening(
                        bookPosition = bookPosition,
                        remaining = remaining,
                        activeTimer = current.activeTimer,
                        isBookPlaying = current.isBookPlaying,
                        bookVolume = current.bookVolume,
                        musicVolume = current.musicVolume
                    )
                }
            }

            is PlaybackState.MusicFadeIn -> {
                val fadeElapsed = current.elapsed + elapsedMs
                val remaining = current.remaining - elapsedMs
                val musicPosition = current.musicPosition + elapsedMs
                val bookPosition = current.bookPosition + elapsedMs

                if (fadeElapsed >= current.duration) {
                    // Fade-in completo -> pasar a MusicPlaying
                    _state.value = PlaybackState.MusicPlaying(
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        remaining = 10_000L, // Inicializado para Tiempo de Música seleccionado (10s por defecto para tests)
                        activeTimer = current.activeTimer,
                        duration = 0L,
                        musicVolume = 1.0f,
                        bookVolume = 0.0f
                    )
                } else {
                    // Continuar fade-in
                    val progress = (fadeElapsed.toFloat() / current.duration.toFloat()).coerceIn(0f, 1f)
                    _state.value = current.copy(
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        remaining = remaining,
                        elapsed = fadeElapsed,
                        musicVolume = progress
                    )
                }
            }

            is PlaybackState.MusicPlaying -> {
                val remaining = current.remaining - elapsedMs
                val musicPosition = current.musicPosition + elapsedMs

                when {
                    // 5 segundos antes de que termine la música -> iniciar BookResume (crossfade)
                    remaining <= 5_000L && remaining > 0 -> {
                        val crossfadeElapsed = (5_000L - remaining)
                        val musicVolume = (remaining.toFloat() / 5_000f).coerceIn(0f, 1f)
                        val bookVolume = 1.0f - musicVolume

                        _state.value = PlaybackState.BookResume(
                            bookPosition = current.bookPosition,
                            musicPosition = musicPosition,
                            remaining = remaining,
                            activeTimer = current.activeTimer,
                            crossfadeElapsed = crossfadeElapsed,
                            crossfadeDuration = 5_000L,
                            bookVolume = bookVolume,
                            musicVolume = musicVolume
                        )
                    }
                    // Música termina sin crossfade (caso límite)
                    remaining <= 0 -> {
                        _state.value = PlaybackState.Idle
                    }
                    else -> {
                        // Actualizar posición de música
                        _state.value = current.copy(
                            musicPosition = musicPosition,
                            remaining = remaining
                        )
                    }
                }
            }

            is PlaybackState.MusicFadeOut -> {
                val fadeElapsed = current.elapsed + elapsedMs
                val remaining = current.remaining - elapsedMs
                val musicPosition = current.musicPosition + elapsedMs

                // 5 segundos antes de terminar el fade-out -> iniciar BookResume
                val triggerMs = current.duration - 5_000L
                if (fadeElapsed >= triggerMs && remaining > 0) {
                    val crossfadeElapsed = fadeElapsed - triggerMs
                    val musicProgress = 1.0f - ((crossfadeElapsed.toFloat() / 5_000f).coerceIn(0f, 1f))
                    val bookProgress = 1.0f - musicProgress

                    _state.value = PlaybackState.BookResume(
                        bookPosition = current.bookPosition,
                        musicPosition = musicPosition,
                        remaining = remaining,
                        activeTimer = current.activeTimer,
                        crossfadeElapsed = crossfadeElapsed,
                        crossfadeDuration = 5_000L,
                        bookVolume = bookProgress,
                        musicVolume = musicProgress
                    )
                } else if (fadeElapsed >= current.duration) {
                    // Fade-out completo -> volver a Listening (ciclo reinicia)
                    _state.value = PlaybackState.Listening(
                        bookPosition = current.bookPosition,
                        remaining = current.activeTimer,
                        activeTimer = current.activeTimer,
                        isBookPlaying = true,
                        bookVolume = 1.0f,
                        musicVolume = 0.0f
                    )
                } else {
                    // Continuar fade-out
                    val progress = (fadeElapsed.toFloat() / current.duration.toFloat()).coerceIn(0f, 1f)
                    val volume = 1.0f - progress

                    _state.value = current.copy(
                        musicPosition = musicPosition,
                        elapsed = fadeElapsed,
                        musicVolume = volume
                    )
                }
            }

            is PlaybackState.BookResume -> {
                val crossfadeElapsed = current.crossfadeElapsed + elapsedMs
                val remaining = current.remaining - elapsedMs
                val musicPosition = current.musicPosition + elapsedMs
                val bookPosition = current.bookPosition + elapsedMs

                if (crossfadeElapsed >= current.crossfadeDuration) {
                    // Crossfade completo -> volver a Listening
                    _state.value = PlaybackState.Listening(
                        bookPosition = bookPosition,
                        remaining = current.activeTimer,
                        activeTimer = current.activeTimer,
                        isBookPlaying = true,
                        bookVolume = 1.0f,
                        musicVolume = 0.0f
                    )
                } else {
                    // Actualizar volúmenes durante crossfade (simétrico)
                    val progress = (crossfadeElapsed.toFloat() / current.crossfadeDuration.toFloat()).coerceIn(0f, 1f)
                    val bookVol = progress
                    val musicVol = 1.0f - progress

                    _state.value = current.copy(
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        crossfadeElapsed = crossfadeElapsed,
                        bookVolume = bookVol,
                        musicVolume = musicVol
                    )
                }
            }

            else -> {
                // Idle o Loading: no hacer nada
            }
        }
    }

    /** Detiene la reproducción y vuelve a Idle. */
    fun stop() {
        _state.value = PlaybackState.Idle
    }

    /** Pausa la reproducción actual (sin cambiar a Idle). */
    fun pause() {
        val current = _state.value
        when (current) {
            is PlaybackState.Listening -> {
                _state.update { current.copy(isBookPlaying = false) }
            }
            is PlaybackState.MusicFadeIn,
            is PlaybackState.MusicPlaying,
            is PlaybackState.MusicFadeOut,
            is PlaybackState.BookResume -> {
                // No se pausa durante fades; dejar que terminen
            }
            else -> { /* Idle/Loading: no hacer nada */ }
        }
    }

    /** Reanuda desde pausa manteniendo el estado actual. */
    fun resume() {
        val current = _state.value
        when (current) {
            is PlaybackState.Listening -> {
                _state.update { current.copy(isBookPlaying = true) }
            }
            else -> { /* No aplicable */ }
        }
    }

    /** Cambia el tiempo de escucha del timer (en tiempo real). */
    fun updateTimer(newTimerMs: Long) {
        val current = _state.value
        when (current) {
            is PlaybackState.Listening -> {
                _state.update {
                    current.copy(
                        activeTimer = newTimerMs,
                        remaining = newTimerMs - (current.activeTimer - current.remaining)
                    )
                }
            }
            else -> { /* No se cambia timer durante fades */ }
        }
    }
}