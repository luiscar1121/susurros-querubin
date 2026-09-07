// app/src/main/java/com/sq/susurros/ui/components/CircularDial.kt
package com.sq.susurros.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterTweak
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDrag: (progress: Float) -> Unit
) {
    val strokeWidthPx = dialStroke.toPx()

    // Colores del tema
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val progressColor = MaterialTheme.colorScheme.primary // #BEFF00
    val borderColor = MaterialTheme.colorScheme.surfaceVariant

    // Estado interno para el gesto de arrastre
    val dragProgress = remember { mutableStateOf(progress) }

    Box(
        modifier = Modifier
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
                detectDragGesturesAfterTweak(
                    onDragStart = { },
                    onDragEnd = {
                        onDrag(dragProgress.value)
                    },
                    onDrag = { change, _ ->
                        val centerX = size.center.x
                        val centerY = size.center.y
                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY

                        // Calcular ángulo desde el centro (0 = arriba, en sentido horario)
                        val angle = atan2(dy, -dx)
                        // Normalizar a [0, 2π] con 0 en la posición superior
                        var normalized = if (angle < 0) angle + (2 * PI) else angle
                        // Ajustar: 0 en la parte superior (como las 12 en un reloj)
                        normalized = (normalized - PI / 2 + 2 * PI) % (2 * PI)

                        val newProgress = (normalized / (2 * PI)).toFloat()
                        dragProgress.value = newProgress
                    }
                )
            }
    ) {
        // Canvas: arco de fondo + progreso
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = (dialSize.toPx() / 2f) - strokeWidthPx / 2f

            // Track de fondo (completo)
            drawCircle(
                color = trackColor,
                center = Offset(center.x, center.y),
                radius = radius,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Arco de progreso
            // El progreso del timer: 0.0 = empezando (arco al 100%)
            // 1.0 = agotado (arco vacío)
            val progressAngle = 360f * (1f - progress.coerceIn(0f, 1f))
            drawArc(
                color = progressColor,
                startAngle = 90f, // comienza en la parte superior
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
        androidx.compose.material3.Surface(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.Center),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = fontSizeCurrent
                )
                androidx.compose.material3.Text(
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
}