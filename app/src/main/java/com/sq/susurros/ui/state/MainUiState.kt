// app/src/main/java/com/sq/susurros/ui/state/MainUiState.kt
package com.sq.susurros.ui.state

import com.sq.susurros.data.model.BookData
import com.sq.susurros.domain.state.PlaybackState

data class MainUiState(
    val bookTitle: String = "Crónica de una Muerte Anunciada",
    val bookAuthor: String = "Gabriel García Márquez",
    val bookDurationMs: Long = 1_200_000L, // 20m
    val bookPosition: Long = 522_000L,     // 08:42

    val activeTimerMs: Long = 60 * 60_000L, // 1h por defecto
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val volume: Float = 0.75f,

    val listenTimeTier: Int = 0, // 0 = 1h
    val musicTimeTier: Int = 1,  // 0 = T.C. (Tema Completo), 1 = 120s

    val musicDurationMs: Long = 120_000L, // por defecto 120s
    val isMusicRandomMode: Boolean = true,
    val books: List<BookData> = emptyList()
) {
    companion object {
        val Initial = MainUiState()
    }
}
