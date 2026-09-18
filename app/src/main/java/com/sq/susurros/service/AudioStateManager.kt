// app/src/main/java/com/sq/susurros/service/AudioStateManager.kt
package com.sq.susurros.service

import com.sq.susurros.domain.state.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

class AudioStateManager {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var musicDurationMs: Long = 120_000L

    fun setMusicDuration(durationMs: Long) {
        this.musicDurationMs = durationMs
    }

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

    fun onTick(deltaMs: Long) {
        val current = _state.value

        when (current) {
            is PlaybackState.Playing -> {
                _state.value = current.copy(
                    bookPosition = max(0L, current.bookPosition + deltaMs)
                )
            }

            is PlaybackState.Listening -> {
                val remaining = current.remaining - deltaMs
                val bookPosition = max(0L, current.bookPosition + deltaMs)

                if (remaining <= 6_000L && remaining > 0 && deltaMs > 0) {
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
                } else if (remaining <= 0 && deltaMs > 0) {
                    _state.value = PlaybackState.Idle
                } else {
                    _state.value = current.copy(
                        bookPosition = bookPosition,
                        remaining = max(0L, remaining)
                    )
                }
            }

            is PlaybackState.MusicFadeIn -> {
                val fadeElapsed = current.elapsed + deltaMs
                val bookPosition = max(0L, current.bookPosition + deltaMs)
                val musicPosition = max(0L, current.musicPosition + deltaMs)

                if (fadeElapsed >= current.duration) {
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
                val bookPosition = max(0L, current.bookPosition + deltaMs)
                val musicPosition = max(0L, current.musicPosition + deltaMs)

                if (remaining <= 0 && deltaMs > 0) {
                    _state.value = PlaybackState.MusicFadeOut(
                        bookPosition = bookPosition,
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
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        remaining = max(0L, remaining)
                    )
                }
            }

            is PlaybackState.MusicFadeOut -> {
                val fadeElapsed = current.elapsed + deltaMs
                val musicPosition = max(0L, current.musicPosition + deltaMs)
                val bookPosition = max(0L, current.bookPosition + deltaMs)

                val triggerMs = current.duration - 5_000L
                if (fadeElapsed >= triggerMs && current !is PlaybackState.BookResume && deltaMs > 0) {
                    val crossfadeElapsed = fadeElapsed - triggerMs
                    val resumePosition = max(0L, bookPosition - 20_000L)

                    _state.value = PlaybackState.BookResume(
                        bookPosition = resumePosition,
                        musicPosition = musicPosition,
                        remaining = max(0L, current.duration - fadeElapsed),
                        activeTimer = current.activeTimer,
                        crossfadeElapsed = crossfadeElapsed,
                        crossfadeDuration = 5_000L,
                        bookVolume = (crossfadeElapsed.toFloat() / 5_000f).coerceIn(0f, 1f),
                        musicVolume = 1.0f - (crossfadeElapsed.toFloat() / 5_000f).coerceIn(0f, 1f)
                    )
                } else if (fadeElapsed >= current.duration && deltaMs > 0) {
                    startListening(bookPosition = bookPosition, timerMs = current.activeTimer)
                } else {
                    val progress = (fadeElapsed.toFloat() / current.duration.toFloat()).coerceIn(0f, 1f)
                    _state.value = current.copy(
                        bookPosition = bookPosition,
                        musicPosition = musicPosition,
                        elapsed = fadeElapsed,
                        musicVolume = 1.0f - progress
                    )
                }
            }

            is PlaybackState.BookResume -> {
                val crossfadeElapsed = current.crossfadeElapsed + deltaMs
                val bookPosition = max(0L, current.bookPosition + deltaMs)
                val musicPosition = max(0L, current.musicPosition + deltaMs)

                if (crossfadeElapsed >= current.crossfadeDuration && deltaMs > 0) {
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

            else -> { }
        }
    }

    fun updateTimer(newTimerMs: Long) {
        val current = _state.value
        if (current is PlaybackState.Listening) {
            _state.value = current.copy(
                remaining = newTimerMs,
                activeTimer = newTimerMs
            )
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
