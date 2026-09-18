// app/src/main/java/com/sq/susurros/ui/components/PlaybackControls.kt
package com.sq.susurros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    volume: Float,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reducido de 16dp
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Reducido de 20dp
    ) {
        // Slider de volumen
        VolumeSlider(
            volume = volume,
            onVolumeChange = onVolumeChange
        )

        // Fila de Controles: Atras 10s — Play/Pause — Adelante 10s
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de retroceso 10s
            SeekControl(
                label = "Atras",
                icon = Icons.Default.Replay10,
                onClick = onSkipBackward
            )

            Spacer(modifier = Modifier.width(32.dp)) // Reducido de 40dp

            // Botón central Play/Pause
            LargePlayButton(
                isPlaying = isPlaying,
                onClick = onPlayPause
            )

            Spacer(modifier = Modifier.width(32.dp)) // Reducido de 40dp

            // Botón de adelanto 10s
            SeekControl(
                label = "Adelante",
                icon = Icons.Default.Forward10,
                onClick = onSkipForward
            )
        }
    }
}

@Composable
private fun LargePlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(64.dp), // Reducido de 72dp
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.Black,
                modifier = Modifier.size(36.dp) // Reducido de 40dp
            )
        }
    }
}

@Composable
private fun SeekControl(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp) // Reducido de 6dp
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(48.dp), // Reducido de 56dp
            shape = CircleShape,
            color = Color(0xFF1E1B4B),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp) // Reducido de 28dp
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp, // Reducido de 11sp
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    val volumeState = remember { mutableStateOf(volume) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeMute, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Slider(
                value = volumeState.value,
                onValueChange = {
                    volumeState.value = it
                    onVolumeChange(it)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    thumbColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f).height(24.dp) // Altura limitada
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }

        Text(
            text = "VOLUMEN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp, // Reducido de 10sp
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
