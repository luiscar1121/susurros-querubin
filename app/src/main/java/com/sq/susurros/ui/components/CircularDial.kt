// app/src/main/java/com/sq/susurros/ui/components/CircularDial.kt
package com.sq.susurros.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Dial de tiempo circular interactivo (Compose).
 *
 * - Visualiza el progreso del "Tiempo de Escucha" como un arco.
 * - Permite arrastrar para seek (seek bar circular).
 * - Muestra tiempo actual y tiempo restante.
 *
 * @param currentTime Texto del tiempo actual (ej: "08:42")
 * @param remainingTime Texto del tiempo restante (ej: "RESTA 03:18")
 * @param progress Progreso del timer (0.0 = empezando, 1.0 = agotado).
 *                 NOTA: el arco visual se invierte (1.0 = completo -> arco al 100%).
 * @param dialSize Tamaño del dial.
 * @param onDrag Callback con el nuevo progreso cuando se arrastra.
 */
@Composable
fun CircularDial(
    currentTime: String,
    remainingTime: String,
    progress: Float,
    dialSize: Dp = 220.dp,
    dialStroke: Dp = 8.dp,
    fontSizeCurrent: TextUnit = 24.sp,
    fontSizeRemaining: TextUnit = 10.sp,
    onDrag: (progress: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { dialStroke.toPx() }

    // Colores del tema
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val progressColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.surfaceVariant

    // Estado interno para el gesto de arrastre
    val dragProgress = remember { mutableStateOf(progress) }
    val dialSizePx = with(density) { dialSize.toPx() }
    val halfSize = dialSizePx / 2f

    Box(
        modifier = modifier
            .size(dialSize)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .border(
                width = 4.dp,
                color = borderColor,
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        onDrag(dragProgress.value)
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val relX = change.position.x.toDouble() - halfSize.toDouble()
                        val relY = change.position.y.toDouble() - halfSize.toDouble()

                        val angle = atan2(relY, -relX)
                        var normalized: Double = if (angle < 0.0) angle + (2.0 * kotlin.math.PI) else angle
                        val halfPI: Double = kotlin.math.PI / 2.0
                        normalized = normalized - halfPI
                        if (normalized < 0.0) normalized += 2.0 * kotlin.math.PI
                        val newProgress = (normalized / (2.0 * kotlin.math.PI)).toFloat().coerceIn(0f, 1f)
                        dragProgress.value = newProgress
                        change.consumeAllChanges()
                    }
                )
            }
    ) {
        // Canvas: arco de fondo + progreso
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = with(density) { dialSize.toPx() / 2f } - strokeWidthPx / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Track de fondo (completo)
            drawCircle(
                color = trackColor,
                center = center,
                radius = radius,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Arco de progreso
            val progressAngle = 360f * (1f - progress.coerceIn(0f, 1f))
            drawArc(
                color = progressColor,
                startAngle = 90f,
                sweepAngle = progressAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )

            // Glow suave alrededor del arco activo
            if (progress < 1f) {
                drawArc(
                    color = progressColor.copy(alpha = 0.3f),
                    startAngle = 90f,
                    sweepAngle = progressAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Contenido central
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = fontSizeCurrent
            )
            Text(
                text = remainingTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = fontSizeRemaining,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}