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
    volume: Float, // Mantener parámetro por compatibilidad con ViewModel pero no usar Slider
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Fila de Controles centralizada: Atras 10s — Play/Pause — Adelante 10s
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón de retroceso 10s
        SeekControl(
            label = "Atras",
            icon = Icons.Default.Replay10,
            onClick = onSkipBackward
        )

        Spacer(modifier = Modifier.width(48.dp))

        // Botón central Play/Pause
        LargePlayButton(
            isPlaying = isPlaying,
            onClick = onPlayPause
        )

        Spacer(modifier = Modifier.width(48.dp))

        // Botón de adelanto 10s
        SeekControl(
            label = "Adelante",
            icon = Icons.Default.Forward10,
            onClick = onSkipForward
        )
    }
}

@Composable
private fun LargePlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color(0xFF1E1B4B),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
