// app/src/main/java/com/sq/susurros/ui/MainScreen.kt
package com.sq.susurros.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sq.susurros.R
import com.sq.susurros.domain.state.PlaybackState
import com.sq.susurros.ui.components.CircularDial
import com.sq.susurros.ui.components.PlaybackControls
import com.sq.susurros.ui.components.TimeSelector
import com.sq.susurros.ui.state.MainUiState
import com.sq.susurros.ui.viewmodel.MainViewModel
import java.util.Locale
import kotlin.math.max

enum class Screen {
    Library, Player, Files, Profile
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(Screen.Player) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SQ 01 Reader",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.height(56.dp)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.height(64.dp)
            ) {
                val items = listOf(
                    Triple(Screen.Library, "Library", R.drawable.ic_notification),
                    Triple(Screen.Player, "Player", R.drawable.ic_play),
                    Triple(Screen.Files, "Files", R.drawable.ic_notification),
                    Triple(Screen.Profile, "Profile", R.drawable.ic_notification)
                )

                items.forEach { (screen, label, iconRes) ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        label = { Text(label, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Library -> LibraryScreen(
                    books = uiState.books,
                    onBookClick = { book ->
                        viewModel.setBookInfo(book.title, book.author ?: "Autor Desconocido", book.durationMs ?: 0L)
                        currentScreen = Screen.Player
                    }
                )
                Screen.Player -> PlayerContent(uiState, viewModel)
                Screen.Files -> FilesScreen(
                    onFileSelected = { book ->
                        viewModel.setBookInfo(book.title, book.author ?: "Autor Desconocido", book.durationMs ?: 0L)
                        currentScreen = Screen.Player
                    }
                )
                Screen.Profile -> ProfileScreen()
            }
        }
    }
}

@Composable
fun PlayerContent(uiState: MainUiState, viewModel: MainViewModel) {
    val listenTimeOptions = listOf("1h", "45m", "30m", "20m")
    val musicTimeOptions = listOf("T.C.", "120s", "90s", "60s")

    val currentTimeText = formatTime(uiState.bookPosition)
    val remainingText = "RESTA ${formatTime(calculateRemaining(uiState))}"

    val progress = if (uiState.activeTimerMs > 0) {
        max(0f, uiState.bookPosition.toFloat() / uiState.bookDurationMs.toFloat())
    } else 0f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BannerAd(modifier = Modifier.height(60.dp))

            HeaderSection(
                title = uiState.bookTitle,
                author = uiState.bookAuthor
            )

            CircularDial(
                currentTime = currentTimeText,
                remainingTime = remainingText,
                progress = progress,
                dialSize = 180.dp, // Reducido de 220dp a 180dp
                onDrag = { viewModel.onSeek(it) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TimeSelector(
                    label = "Tiempo de escucha",
                    options = listenTimeOptions,
                    selectedIndex = uiState.listenTimeTier,
                    onSelect = { viewModel.onSelectListenTime(it) }
                )

                TimeSelector(
                    label = "Intermedio Música",
                    options = musicTimeOptions,
                    selectedIndex = uiState.musicTimeTier,
                    onSelect = { viewModel.onSelectMusicTime(it) }
                )
            }

            PlaybackControls(
                isPlaying = uiState.isPlaying,
                volume = uiState.volume,
                onPlayPause = { viewModel.onPlayPause() },
                onSkipForward = { viewModel.onSkipForward() },
                onSkipBackward = { viewModel.onSkipBackward() },
                onVolumeChange = { viewModel.onVolumeChange(it) }
            )
        }
    }
}

@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SPONSOR SQ PREMIUM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Unleash Ultra-HD Voice Quality Now",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeaderSection(
    title: String,
    author: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp) // Reducido de 72dp
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_notification),
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                fontSize = 15.sp
            )
            Text(
                text = author,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

private fun calculateRemaining(state: MainUiState): Long {
    return state.playbackState.remaining.let { if (it > 0) it else state.activeTimerMs }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = (totalSec / 60) % 60
    val sec = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
