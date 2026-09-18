// app/src/main/java/com/sq/susurros/ui/viewmodel/MainViewModel.kt
package com.sq.susurros.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sq.susurros.data.repository.LibroRepository
import com.sq.susurros.domain.state.PlaybackState
import com.sq.susurros.domain.state.isPlaying
import com.sq.susurros.service.AudioPlaybackService
import com.sq.susurros.service.AudioStateManager
import com.sq.susurros.ui.state.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.common.util.UnstableApi
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libroRepository: LibroRepository,
    private val stateManager: AudioStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState.Initial)
    val uiState: StateFlow<MainUiState> = combine(
        _uiState,
        stateManager.state
    ) { currentUi, playback ->
        currentUi.copy(
            bookPosition = playback.bookPosition,
            isPlaying = playback.isPlaying,
            playbackState = playback
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState.Initial)

    init {
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            try {
                val books = withContext(Dispatchers.IO) {
                    libroRepository.scanBooks()
                }
                _uiState.update { it.copy(books = books) }
            } catch (e: Exception) { }
        }
    }

    @UnstableApi
    fun onPlayPause() {
        // Asegurar que el servicio está iniciado
        val intent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_START
        }
        context.startForegroundService(intent)

        if (uiState.value.isPlaying) {
            stateManager.pause()
        } else {
            if (stateManager.state.value is PlaybackState.Idle) {
                stateManager.startListening(
                    bookPosition = uiState.value.bookPosition,
                    timerMs = uiState.value.activeTimerMs
                )
            } else {
                stateManager.resume()
            }
        }
    }

    fun onSeek(progress: Float) {
        val newPosition = (progress * uiState.value.bookDurationMs).toLong()
        _uiState.update { it.copy(bookPosition = newPosition) }
    }

    fun onSkipForward() {
        stateManager.onTick(10_000L)
    }

    fun onSkipBackward() {
        stateManager.onTick(-10_000L)
    }

    fun onSelectListenTime(tierIndex: Int) {
        val times = listOf(60 * 60_000L, 45 * 60_000L, 30 * 60_000L, 20 * 60_000L)
        val selectedTime = times[tierIndex]
        _uiState.update { it.copy(activeTimerMs = selectedTime, listenTimeTier = tierIndex) }
        stateManager.updateTimer(selectedTime)
    }

    fun onSelectMusicTime(tierIndex: Int) {
        val times = listOf(0L, 120_000L, 90_000L, 60_000L)
        val duration = times[tierIndex]
        _uiState.update { it.copy(musicDurationMs = duration, musicTimeTier = tierIndex) }
        stateManager.setMusicDuration(duration)
    }

    fun onVolumeChange(volume: Float) {
        _uiState.update { it.copy(volume = volume) }
    }

    fun setBookInfo(title: String, author: String, duration: Long) {
        _uiState.update {
            it.copy(
                bookTitle = title,
                bookAuthor = author,
                bookDurationMs = if (duration > 0) duration else 1_200_000L
            )
        }
    }
}
