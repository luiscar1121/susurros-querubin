// app/src/main/java/com/sq/susurros/ui/viewmodel/MainViewModel.kt
package com.sq.susurros.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sq.susurros.data.repository.LibroRepository
import com.sq.susurros.domain.state.PlaybackState
import com.sq.susurros.ui.state.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val libroRepository: LibroRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState.Initial)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            try {
                val books = libroRepository.scanBooks()
                _uiState.update { it.copy(books = books) }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun onPlayPause() {
        _uiState.update { current ->
            if (current.isPlaying) {
                current.copy(
                    isPlaying = false,
                    playbackState = PlaybackState.Idle
                )
            } else {
                current.copy(
                    isPlaying = true,
                    playbackState = PlaybackState.Listening(
                        bookPosition = current.bookPosition,
                        remaining = current.activeTimerMs,
                        activeTimer = current.activeTimerMs,
                        isBookPlaying = true,
                        bookVolume = 1.0f,
                        musicVolume = 0.0f
                    )
                )
            }
        }
    }

    fun onSeek(progress: Float) {
        val newPosition = (progress * _uiState.value.bookDurationMs).toLong()
        _uiState.update { it.copy(bookPosition = newPosition) }
    }

    fun onSelectListenTime(tierIndex: Int) {
        val times = listOf(60 * 60_000L, 45 * 60_000L, 30 * 60_000L, 20 * 60_000L)
        val selectedTime = times[tierIndex]
        _uiState.update { current ->
            val newState = when (val ps = current.playbackState) {
                is PlaybackState.Listening -> ps.copy(
                    remaining = selectedTime,
                    activeTimer = selectedTime
                )
                else -> ps
            }
            current.copy(
                activeTimerMs = selectedTime,
                listenTimeTier = tierIndex,
                playbackState = newState
            )
        }
    }

    fun onSelectMusicTime(tierIndex: Int) {
        val times = listOf(0L, 120_000L, 90_000L, 60_000L)
        _uiState.update {
            it.copy(
                musicDurationMs = times[tierIndex],
                musicTimeTier = tierIndex
            )
        }
    }

    fun onVolumeChange(volume: Float) {
        _uiState.update { it.copy(volume = volume) }
    }

    fun setBookInfo(title: String, author: String, duration: Long) {
        _uiState.update {
            it.copy(
                bookTitle = title,
                bookAuthor = author,
                bookDurationMs = if (duration > 0) duration else 1_200_000L // 20m default if zero
            )
        }
    }
}
