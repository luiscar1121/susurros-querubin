// app/src/main/java/com/sq/susurros/service/AudioStateManager.kt
package com.sq.susurros.service

import com.sq.susurros.domain.state.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

/**
 * AudioStateManager — Máquina de estados de reproducción (Versión 02).
 */
class AudioStateManager {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Duración por defecto del "Tiempo de Música" si no se especifica. */
    private var musicDurationMs: Long = 120_000L

    fun setMusicDuration(durationMs: Long) {
        this.musicDurationMs = durationMs
    }

    /** Inicia la reproducción básica del libro. */
    fun startPlaying(bookPosition: Long = 0L) {
        _state.value = PlaybackState.Playing(bookPosition = bookPosition)
    }

    /** Inicia la fase de escucha con temporizador. */
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
     * Procesa un tick periódico y orquesta los fades según las reglas "02".
     * @param deltaMs Tiempo transcurrido desde el último tick.
     */
    fun onTick(deltaMs: Long) {
        val current = _state.value

        when (current) {
            is PlaybackState.Playing -> {
                _state.value = current.copy(
                    bookPosition = current.bookPosition + deltaMs
                )
            }

            is PlaybackState.Listening -> {
                val remaining = current.remaining - deltaMs
                val bookPosition = current.bookPosition + deltaMs

                // 6 segundos ANTES del fin -> iniciar MusicFadeIn
                if (remaining <= 6_000L && remaining > 0) {
                    val overflow = 6_000L - remaining
                    _state.value = PlaybackState.MusicFadeIn(
                        bookPosition = bookPosition,
                        remaining = remaining,
                        activeTimer = current.activeTimer,
                        musicPosition = 0L,
                        elapsed = overflow,
                        duration = 10_000L,
                        bookVolume = 1.0f,
                        musicVolume = (overflow.toFloat() / 10_000f).coerceIn(0f, 1f)
                    )
                } else if (remaining <= 0) {
                    // Si no hubo trigger de fade (timer muy corto), ir a Idle o terminar frase
                    _state.value = PlaybackState.Idle
                } else {
                    _state.value = current.copy(
                        bookPosition = bookPosition,
                        remaining = remaining
                    )
                }
            }

            is PlaybackState.MusicFadeIn -> {
                val fadeElapsed = current.elapsed + deltaMs
                val bookPosition = current.bookPosition + deltaMs
                val musicPosition = current.musicPosition + deltaMs

                if (fadeElapsed >= current.duration) {
                    // Fade-in completo -> pasar a MusicPlaying
                    _state.value = PlaybackState.MusicPlaying(
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        remaining = musicDurationMs,
                        activeTimer = current.activeTimer,
                        duration = musicDurationMs,
                        musicVolume = 1.0f,
                        bookVolume = 0.0f
                    )
                } else {
                    val progress = (fadeElapsed.toFloat() / current.duration.toFloat()).coerceIn(0f, 1f)
                    _state.value = current.copy(
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        elapsed = fadeElapsed,
                        musicVolume = progress
                    )
                }
            }

            is PlaybackState.MusicPlaying -> {
                val remaining = current.remaining - deltaMs
                val musicPosition = current.musicPosition + deltaMs

                if (remaining <= 0) {
                    // Música terminada -> iniciar FadeOut
                    _state.value = PlaybackState.MusicFadeOut(
                        bookPosition = current.bookPosition,
                        musicPosition = musicPosition,
                        remaining = 0L,
                        activeTimer = current.activeTimer,
                        elapsed = 0L,
                        duration = 10_000L,
                        musicVolume = 1.0f,
                        bookVolume = 0.0f
                    )
                } else {
                    _state.value = current.copy(
                        musicPosition = musicPosition,
                        remaining = remaining
                    )
                }
            }

            is PlaybackState.MusicFadeOut -> {
                val fadeElapsed = current.elapsed + deltaMs
                val musicPosition = current.musicPosition + deltaMs

                // 5 segundos ANTES de que termine la música -> iniciar BookResume (crossfade)
                val triggerMs = current.duration - 5_000L
                if (fadeElapsed >= triggerMs && current !is PlaybackState.BookResume) {
                    val crossfadeElapsed = fadeElapsed - triggerMs
                    
                    // REGLA: El libro comienza 20 segundos ANTES de la última posición de pausa.
                    val resumePosition = max(0L, current.bookPosition - 20_000L)

                    _state.value = PlaybackState.BookResume(
                        bookPosition = resumePosition,
                        musicPosition = musicPosition,
                        remaining = current.duration - fadeElapsed,
                        activeTimer = current.activeTimer,
                        crossfadeElapsed = crossfadeElapsed,
                        crossfadeDuration = 5_000L,
                        bookVolume = (crossfadeElapsed.toFloat() / 5_000f).coerceIn(0f, 1f),
                        musicVolume = 1.0f - (crossfadeElapsed.toFloat() / 5_000f).coerceIn(0f, 1f)
                    )
                } else if (fadeElapsed >= current.duration) {
                    // Ciclo reinicia -> Volver a Playing/Listening
                    startListening(bookPosition = current.bookPosition, timerMs = current.activeTimer)
                } else {
                    val progress = (fadeElapsed.toFloat() / current.duration.toFloat()).coerceIn(0f, 1f)
                    _state.value = current.copy(
                        musicPosition = musicPosition,
                        elapsed = fadeElapsed,
                        musicVolume = 1.0f - progress
                    )
                }
            }

            is PlaybackState.BookResume -> {
                val crossfadeElapsed = current.crossfadeElapsed + deltaMs
                val bookPosition = current.bookPosition + deltaMs
                val musicPosition = current.musicPosition + deltaMs

                if (crossfadeElapsed >= current.crossfadeDuration) {
                    // Crossfade completo -> Volver a Listening (reinicio de contador)
                    startListening(bookPosition = bookPosition, timerMs = current.activeTimer)
                } else {
                    val progress = (crossfadeElapsed.toFloat() / current.crossfadeDuration.toFloat()).coerceIn(0f, 1f)
                    _state.value = current.copy(
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        crossfadeElapsed = crossfadeElapsed,
                        bookVolume = progress,
                        musicVolume = 1.0f - progress
                    )
                }
            }

            else -> { /* Idle / Loading: no tick */ }
        }
    }

    fun pause() {
        val current = _state.value
        if (current is PlaybackState.Listening) {
            _state.value = current.copy(isBookPlaying = false)
        }
    }

    fun resume() {
        val current = _state.value
        if (current is PlaybackState.Listening) {
            _state.value = current.copy(isBookPlaying = true)
        }
    }

    fun stop() {
        _state.value = PlaybackState.Idle
    }
}
