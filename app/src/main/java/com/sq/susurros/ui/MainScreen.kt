// app/src/main/java/com/sq/susurros/ui/MainScreen.kt
package com.sq.susurros.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sq.susurros.R
import com.sq.susurros.ui.components.CircularDial
import com.sq.susurros.ui.components.PlaybackControls
import com.sq.susurros.ui.components.TimeSelector
import com.sq.susurros.ui.state.MainUiState
import com.sq.susurros.ui.viewmodel.MainViewModel
import kotlin.math.max

/**
 * MainScreen — Composición principal del lector.
 *
 * Layout sin scroll usando ConstraintLayout (según especificación del IDEA.md):
 * 1. Banner publicidad (top)
 * 2. Logo + título del libro
 * 3. Dial de tiempo circular (seek bar)
 * 4. Selectores de tiempo (Tiempo de escucha + Intermedio música)
 * 5. Controles de reproducción (retroceso, play/pause, adelanto, volumen)
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState = viewModel.uiState.value

    // Opciones de tiempo (en milisegundos)
    val listenTimeOptions = listOf("1h", "45m", "30m", "20m")
    val musicTimeOptions = listOf("T.C.", "120s", "90s", "60s")

    // Formatear tiempo para el dial
    val currentTimeText = formatTime(uiState.bookPosition)
    val remainingText = "RESTA ${formatTime(calculateRemaining(uiState))}"

    // Progreso del dial: 1.0 = empezando, 0.0 = agotado (inverso al reloj)
    val progress = if (uiState.activeTimerMs > 0) {
        max(0f, uiState.bookPosition.toFloat() / uiState.bookDurationMs.toFloat())
    } else 0f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val (
                banner,
                header,
                dial,
                listenTime,
                musicTime,
                controls
            ) = createRefs()

            // 1. Banner de publicidad
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .constrainAs(banner) {
                        top.linkTo(parent.top, margin = 8.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
            )

            // 2. Logo + título
            HeaderSection(
                title = uiState.bookTitle,
                author = uiState.bookAuthor,
                modifier = Modifier.constrainAs(header) {
                    top.linkTo(banner.bottom, margin = 12.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fill
                }
            )

            // 3. Dial de tiempo circular
            CircularDial(
                currentTime = currentTimeText,
                remainingTime = remainingText,
                progress = progress,
                onDrag = { newProgress ->
                    val newPos = (newProgress * uiState.bookDurationMs).toLong()
                    viewModel.onSeek(newProgress)
                },
                modifier = Modifier.constrainAs(dial) {
                    top.linkTo(header.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.value(220.dp)
                    height = Dimension.value(220.dp)
                }
            )

            // 4a. Selectores de tiempo de escucha
            TimeSelector(
                label = "Tiempo de escucha",
                options = listenTimeOptions,
                selectedIndex = uiState.listenTimeTier,
                onSelect = { index -> viewModel.onSelectListenTime(index) },
                modifier = Modifier.constrainAs(listenTime) {
                    top.linkTo(dial.bottom, margin = 12.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fill
                }
            )

            // 4b. Selectores de tiempo de música
            TimeSelector(
                label = "Intermedio Música",
                options = musicTimeOptions,
                selectedIndex = uiState.musicTimeTier,
                onSelect = { index -> viewModel.onSelectMusicTime(index) },
                modifier = Modifier.constrainAs(musicTime) {
                    top.linkTo(listenTime.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fill
                }
            )

            // 5. Controles de reproducción
            PlaybackControls(
                isPlaying = uiState.isPlaying,
                volume = uiState.volume,
                onPlayPause = { viewModel.onPlayPause() },
                onSkipForward = { viewModel.onSeek((uiState.bookPosition + 10_000L).toFloat() / uiState.bookDurationMs) },
                onSkipBackward = { viewModel.onSeek(maxOf(0f, (uiState.bookPosition - 10_000L).toFloat() / uiState.bookDurationMs) },
                onVolumeChange = { viewModel.onVolumeChange(it) },
                modifier = Modifier.constrainAs(controls) {
                    top.linkTo(musicTime.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fill
                }
            )
        }
    }
}

/**
 * Banner de publicidad (top).
 * Fondo difuminado con texto del patrocinador.
 */
@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SPONSOR SQ PREMIUM",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Sección del encabezado: logo + título del libro + autor.
 */
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
        // Logo (placeholder con background surface)
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_notification),
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Calcula el tiempo restante basado en el estado actual.
 */
private fun calculateRemaining(state: MainUiState): Long {
    val progressFraction = if (state.bookDurationMs > 0) {
        state.bookPosition.toFloat() / state.bookDurationMs.toFloat()
    } else 0f
    return ((1f - progressFraction) * state.bookDurationMs).toLong()
}

/**
 * Formatea milisegundos como MM:SS.
 */
private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = (totalSec / 60) % 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
