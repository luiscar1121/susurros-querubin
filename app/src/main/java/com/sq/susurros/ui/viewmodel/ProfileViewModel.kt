// app/src/main/java/com/sq/susurros/ui/viewmodel/ProfileViewModel.kt
package com.sq.susurros.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.sq.susurros.data.model.VoiceData
import com.sq.susurros.data.repository.VozRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val vozRepository: VozRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val voices = vozRepository.listVoices()
        _uiState.update { it.copy(availableVoices = voices) }
    }

    fun onLanguageChange(lang: String) {
        _uiState.update { it.copy(selectedLanguage = lang) }
        vozRepository.initTts(if (lang == "Spanish") "es-ES" else "en-US")
    }

    fun onSpeedChange(speed: Float) {
        _uiState.update { it.copy(speed = speed) }
        vozRepository.setVoiceParams(speed, _uiState.value.pitch)
    }

    fun onPitchChange(pitch: Float) {
        _uiState.update { it.copy(pitch = pitch) }
        vozRepository.setVoiceParams(_uiState.value.speed, pitch)
    }

    fun onVoiceSelected(voiceId: String) {
        _uiState.update { it.copy(selectedVoiceId = voiceId) }
        // En real, aplicar la voz específica al motor TTS
    }
}

data class ProfileUiState(
    val selectedLanguage: String = "Spanish",
    val speed: Float = 1.2f,
    val pitch: Float = 1.0f,
    val selectedVoiceId: String = "elena_es",
    val availableVoices: List<VoiceData> = emptyList()
)
