// app/src/main/java/com/sq/susurros/ui/viewmodel/FilesViewModel.kt
package com.sq.susurros.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sq.susurros.data.model.BookData
import com.sq.susurros.data.model.MusicData
import com.sq.susurros.data.repository.LibroRepository
import com.sq.susurros.data.repository.MusicaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val libroRepository: LibroRepository,
    private val musicaRepository: MusicaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val selectedUris = mutableSetOf<Uri>()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val books = libroRepository.scanBooks()
                val music = musicaRepository.scanMusic()
                val internalMusic = musicaRepository.getInternalMusicTracks()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        books = books,
                        foundMusic = music,
                        savedMusic = internalMusic,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun onBookFileSelected(uri: Uri) {
        viewModelScope.launch {
            libroRepository.saveBookToInternal(uri)
            loadAll()
        }
    }

    fun toggleMusicSelection(uri: Uri) {
        if (selectedUris.contains(uri)) {
            selectedUris.remove(uri)
        } else {
            selectedUris.add(uri)
        }
        _uiState.update { it.copy(selectedMusicCount = selectedUris.size) }
    }

    fun saveSelectedMusic() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            selectedUris.forEach { uri ->
                musicaRepository.saveMusicToInternal(uri)
            }
            selectedUris.clear()
            _uiState.update { it.copy(selectedMusicCount = 0) }
            loadAll()
        }
    }

    fun onMusicFileSelected(uri: Uri) {
        viewModelScope.launch {
            musicaRepository.saveMusicToInternal(uri)
            loadAll()
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            libroRepository.deleteBook(bookId)
            loadAll()
        }
    }

    fun deleteInternalMusic(file: File) {
        viewModelScope.launch {
            musicaRepository.deleteInternalTrack(file)
            loadAll()
        }
    }
}

data class FilesUiState(
    val isLoading: Boolean = false,
    val books: List<BookData> = emptyList(),
    val foundMusic: List<MusicData> = emptyList(),
    val savedMusic: List<File> = emptyList(),
    val selectedMusicCount: Int = 0,
    val error: String? = null
)
