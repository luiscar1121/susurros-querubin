// app/src/main/java/com/sq/susurros/ui/viewmodel/MainViewModel.kt
package com.sq.susurros.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sq.susurros.domain.state.PlaybackState
import com.sq.susurros.ui.state.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainViewModel — expone un único StateFlow<MainUiState> reactivo.
 *
 * Lee el estado de AudioStateManager (servicio) y lo combina con la
 * configuración del timer para emitir MainUiState a la UI.
 *
 * Nota: El acceso directo al AudioStateManager del servicio requiere
 * un enlace (ServiceConnection). Aquí se expone una referencia simple.
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState.Initial)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Escuchar cambios del estado de reproducción (simulado desde ViewModel)
        viewModelScope.launch {
            // En la implementación completa, esto conectaría con el servicio
            // via ServiceConnection o un repository compartido.
        }
    }

    fun onPlayPause() {
        // En implementación completa: comunicar al servicio
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
        _uiState.update {
            it.copy(
                activeTimerMs = times[tierIndex],
                listenTimeTier = tierIndex
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

    fun updatePlaybackState(newState: PlaybackState) {
        _uiState.update {
            it.copy(playbackState = newState)
        }
    }

    fun setBookInfo(title: String, author: String, duration: Long) {
        _uiState.update {
            it.copy(
                bookTitle = title,
                bookAuthor = author,
                bookDurationMs = duration
            )
        }
    }
}
