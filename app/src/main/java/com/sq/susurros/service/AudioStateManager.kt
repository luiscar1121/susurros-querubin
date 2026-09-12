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
     * @param elapsedMs Tiempo transcurrido desde que se inició el estado actual.
     */
    fun onTick(elapsedMs: Long) {
        val current = _state.value

        when (current) {
            is PlaybackState.Listening -> {
                val remaining = current.activeTimer - elapsedMs
                val bookPosition = current.bookPosition + elapsedMs

                // 6 segundos ANTES del fin del Timer de Escucha -> iniciar MusicFadeIn
                val triggerMs = current.activeTimer - 6_000L
                if (elapsedMs >= triggerMs && remaining > 0) {
                    _state.update {
                        PlaybackState.MusicFadeIn(
                            bookPosition = bookPosition,
                            remaining = remaining,
                            activeTimer = current.activeTimer,
                            musicPosition = 0L,
                            elapsed = elapsedMs - triggerMs,
                            duration = 10_000L,
                            targetVolume = 1.0f,
                            bookVolume = 1.0f,
                            musicVolume = (((elapsedMs - triggerMs).toFloat() / 10_000f) * 1.0f).coerceAtMost(1.0f)
                        )
                    }
                }
                // Si ya agotó el timer -> buscar punto final de frase y terminar
                else if (remaining <= 0) {
                    _state.value = PlaybackState.Idle
                }
            }

            is PlaybackState.MusicFadeIn -> {
                val fadeElapsed = current.elapsed + elapsedMs
                val remaining = current.remaining - elapsedMs

                if (fadeElapsed >= current.duration) {
                    // Fade-in completo -> pasar a MusicPlaying
                    _state.update {
                        PlaybackState.MusicPlaying(
                            bookPosition = current.bookPosition,
                            musicPosition = current.musicPosition + elapsedMs,
                            remaining = remaining,
                            activeTimer = current.activeTimer,
                            duration = 0L,
                            musicVolume = 1.0f,
                            bookVolume = 0.0f
                        )
                    }
                } else {
                    // Continuar fade-in
                    val progress = (fadeElapsed.toFloat() / current.duration.toFloat()).coerceAtMost(1.0f)
                    _state.update {
                        current.copy(
                            musicPosition = current.musicPosition + elapsedMs,
                            elapsed = fadeElapsed,
                            musicVolume = progress
                        )
                    }
                }
            }

            is PlaybackState.MusicPlaying -> {
                val remaining = current.remaining - elapsedMs

                when {
                    // 5 segundos antes de que termine la música -> iniciar BookResume (crossfade)
                    remaining <= 5_000L && remaining > 0 -> {
                        val crossfadeElapsed = (5_000L - remaining)
                        val musicVolume = (remaining.toFloat() / 5_000f).coerceIn(0f, 1f)
                        val bookVolume = 1.0f - musicVolume

                        _state.update {
                            PlaybackState.BookResume(
                                bookPosition = current.bookPosition,
                                musicPosition = current.musicPosition + elapsedMs,
                                remaining = remaining,
                                activeTimer = current.activeTimer,
                                crossfadeElapsed = crossfadeElapsed,
                                crossfadeDuration = 5_000L,
                                bookVolume = bookVolume,
                                musicVolume = musicVolume
                            )
                        }
                    }
                    // Música termina sin crossfade (caso límite)
                    remaining <= 0 -> {
                        _state.value = PlaybackState.Idle
                    }
                    else -> {
                        // Actualizar posición de música
                        _state.update {
                            current.copy(
                                musicPosition = current.musicPosition + elapsedMs,
                                remaining = remaining
                            )
                        }
                    }
                }
            }

            is PlaybackState.MusicFadeOut -> {
                val fadeElapsed = current.elapsed + elapsedMs
                val remaining = current.remaining - elapsedMs

                // 5 segundos antes de terminar el fade-out -> iniciar BookResume
                val triggerMs = current.duration - 5_000L
                if (fadeElapsed >= triggerMs && remaining > 0) {
                    val crossfadeElapsed = fadeElapsed - triggerMs
                    val musicProgress = 1.0f - ((crossfadeElapsed.toFloat() / 5_000f).coerceAtMost(1.0f))
                    val bookProgress = 1.0f - musicProgress

                    _state.update {
                        PlaybackState.BookResume(
                            bookPosition = current.bookPosition,
                            musicPosition = current.musicPosition + elapsedMs,
                            remaining = remaining,
                            activeTimer = current.activeTimer,
                            crossfadeElapsed = crossfadeElapsed,
                            crossfadeDuration = 5_000L,
                            bookVolume = bookProgress,
                            musicVolume = musicProgress
                        )
                    }
                } else if (fadeElapsed >= current.duration) {
                    // Fade-out completo -> volver a Listening (ciclo reinicia)
                    _state.update {
                        PlaybackState.Listening(
                            bookPosition = current.bookPosition,
                            remaining = current.activeTimer,
                            activeTimer = current.activeTimer,
                            isBookPlaying = true,
                            bookVolume = 1.0f,
                            musicVolume = 0.0f
                        )
                    }
                } else {
                    // Continuar fade-out
                    val progress = (fadeElapsed.toFloat() / current.duration.toFloat()).coerceAtMost(1.0f)
                    val volume = 1.0f - progress

                    _state.update {
                        current.copy(
                            musicPosition = current.musicPosition + elapsedMs,
                            elapsed = fadeElapsed,
                            musicVolume = volume
                        )
                    }
                }
            }

            is PlaybackState.BookResume -> {
                val crossfadeElapsed = current.crossfadeElapsed + elapsedMs
                val remaining = current.remaining - elapsedMs

                if (crossfadeElapsed >= current.crossfadeDuration) {
                    // Crossfade completo -> volver a Listening
                    _state.update {
                        PlaybackState.Listening(
                            bookPosition = current.bookPosition + crossfadeElapsed,
                            remaining = current.remaining - crossfadeElapsed,
                            activeTimer = current.activeTimer,
                            isBookPlaying = true,
                            bookVolume = 1.0f,
                            musicVolume = 0.0f
                        )
                    }
                } else {
                    // Actualizar volúmenes durante crossfade (simétrico)
                    val progress = (crossfadeElapsed.toFloat() / current.crossfadeDuration.toFloat()).coerceAtMost(1.0f)
                    val bookVol = progress
                    val musicVol = 1.0f - progress

                    _state.update {
                        current.copy(
                            crossfadeElapsed = crossfadeElapsed,
                            bookVolume = bookVol,
                            musicVolume = musicVol
                        )
                    }
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