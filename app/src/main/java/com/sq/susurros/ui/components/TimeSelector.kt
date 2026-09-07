// app/src/main/java/com/sq/susurros/ui/components/TimeSelector.kt
package com.sq.susurros.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Selectores de tiempo con AssistChips para "Tiempo de Escucha" y "Intermedio Música".
 *
 * - listenTimeOptions: [1h, 45m, 30m, 20m]
 * - musicTimeOptions:  [T.C., 120s, 90s, 60s]
 *
 * @param selectedIndex Índice del chip seleccionado (0-3).
 * @param onSelect Callback con el índice del chip seleccionado.
 */
@Composable
fun TimeSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, optionLabel ->
                TimeChip(
                    text = optionLabel,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    selectedColor = activeColor,
                    unselectedColor = inactiveColor,
                )
            }
        }
    }
}

@Composable
private fun TimeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) selectedColor else unselectedColor,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        border = null,
        modifier = Modifier.width(0.dp).weight(1f)
    )
}