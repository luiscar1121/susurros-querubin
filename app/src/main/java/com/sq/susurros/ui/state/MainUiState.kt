// app/src/main/java/com/sq/susurros/ui/state/MainUiState.kt
package com.sq.susurros.ui.state

import com.sq.susurros.data.model.BookData
import com.sq.susurros.domain.state.PlaybackState

data class MainUiState(
    val bookTitle: String,
    val bookAuthor: String,
    val bookDurationMs: Long,
    val bookPosition: Long,
    val currentBookPath: String? = null,
    
    val activeTimerMs: Long,
    val isPlaying: Boolean,
    val playbackState: PlaybackState,
    val volume: Float,

    val listenTimeTier: Int,
    val musicTimeTier: Int,

    val musicDurationMs: Long,
    val isMusicRandomMode: Boolean,
    val books: List<BookData>
) {
    companion object {
        val Initial = MainUiState(
            bookTitle = "Selecciona un libro",
            bookAuthor = "---",
            bookDurationMs = 1,
            bookPosition = 0L,
            activeTimerMs = 30 * 60_000L,
            isPlaying = false,
            playbackState = PlaybackState.Idle,
            volume = 1.0f,
            listenTimeTier = 2,
            musicTimeTier = 1,
            musicDurationMs = 120_000L,
            isMusicRandomMode = true,
            books = emptyList()
        )
    }
}
