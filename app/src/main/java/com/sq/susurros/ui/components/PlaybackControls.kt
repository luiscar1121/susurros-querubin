// app/src/main/java/com/sq/susurros/ui/components/PlaybackControls.kt
package com.sq.susurros.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sq.susurros.R

/**
 * Controles de reproducción: Play/Pause (FAB), retroceso 10s, adelanto 10s, volumen.
 *
 * Inspirado en el diseño de "sq 01 reader.html":
 * - Botón Play/Pause central (FloatingActionButton)
 * - Botones de retroceso/adelanto en los lados
 * - Slider de volumen horizontal
 */
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
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Controles: retroceso — Play/Pause — adelanto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de retroceso 10s
            ControlButton(
                iconRes = R.drawable.ic_skip_previous,
                contentDesc = "Retroceso 10s",
                onClick = onSkipBackward
            )

            Spacer(modifier = Modifier.size(8.dp))

            // Botón Play/Pause (FloatingActionButton)
            FloatingActionButton(
                onClick = onPlayPause,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Botón de adelanto 10s
            ControlButton(
                iconRes = R.drawable.ic_skip_next,
                contentDesc = "Adelanto 10s",
                onClick = onSkipForward
            )
        }

        // Slider de volumen
        VolumeSlider(
            volume = volume,
            onVolumeChange = onVolumeChange
        )
    }
}

@Composable
private fun ControlButton(
    iconRes: Int,
    contentDesc: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDesc },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDesc,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
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
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Volumen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
    }
}