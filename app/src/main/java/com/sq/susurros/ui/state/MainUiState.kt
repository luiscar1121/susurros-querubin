// app/src/main/java/com/sq/susurros/ui/state/MainUiState.kt
package com.sq.susurros.ui.state

import com.sq.susurros.domain.state.PlaybackState

/**
 * Estado inmutable de la UI, expuesto vía StateFlow desde MainViewModel.
 *
 * @property bookTitle Título del libro actual.
 * @property bookAuthor Autor del libro actual.
 * @property bookDurationMs Duración total del libro/audio en ms.
 * @property bookPosition Posición actual de reproducción del libro en ms.
 * @property activeTimerMs Duración del "Tiempo de Escucha" seleccionado.
 * @property isPlaying Indica si hay algo reproduciéndose.
 * @property playbackState Estado actual de la máquina de estados.
 * @property volume Volumen principal.
 * @property listenTimeTier Índice seleccionado de los chips de tiempo de escucha (0-3).
 * @property musicTimeTier Índice seleccionado de los chips de tiempo de música (0-3).
 * @property musicDurationMs Duración del "Tiempo de Música" seleccionado.
 */
data class MainUiState(
    val bookTitle: String = "Crónica de una Muerte Anunciada",
    val bookAuthor: String = "Gabriel García Márquez",
    val bookDurationMs: Long = 282_000L, // ~4m 42s (8:42)
    val bookPosition: Long = 522_000L,  // posición actual (en ms)

    val activeTimerMs: Long = 60 * 60_000L, // 1h por defecto
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val volume: Float = 0.75f,

    val listenTimeTier: Int = 0, // 0 = 1h
    val musicTimeTier: Int = 1,  // 0 = T.C. (Tema Completo), 1 = 120s

    val musicDurationMs: Long = 120_000L, // por defecto 120s
    val isMusicRandomMode: Boolean = true
) {
    companion object {
        val Initial = MainUiState()
    }
}
