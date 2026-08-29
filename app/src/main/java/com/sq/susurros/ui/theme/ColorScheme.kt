// app/src/main/java/com/sq/susurros/ui/theme/ColorScheme.kt
package com.sq.susurros.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta del DESIGN.md adaptada a Compose ColorScheme.
 *
 * Paleta principal (Sonic Clarity):
 * - Background: #0b1326 (charcoal-navy)
 * - Primary:    #BEFF00 (electric lime)
 * - Secondary:  #B794FF (soft lavender)
 */
val LightColors = lightColorScheme(
    primary = Color(0xFFBEFF00),
    onPrimary = Color(0xFF253500),
    primaryContainer = Color(0xFFb8f600),
    onPrimaryContainer = Color(0xFF506e00),
    inversePrimary = Color(0xFF4b6700),

    secondary = Color(0xFFd2bbff),
    onSecondary = Color(0xFF38255e),
    secondaryContainer = Color(0xFF523e79),
    onSecondaryContainer = Color(0xFFc4adf0),

    tertiary = Color(0xFFffffff),
    onTertiary = Color(0xFF2f3131),
    tertiaryContainer = Color(0xFFe2e2e2),
    onTertiaryContainer = Color(0xFF636565),

    background = Color(0xFF0b1326),
    onBackground = Color(0xFFdae2fd),

    surface = Color(0xFF0B1326),
    onSurface = Color(0xFFdae2fd),
    surfaceVariant = Color(0xFF2d3449),
    onSurfaceVariant = Color(0xFFc4c7c8),

    surfaceContainer = Color(0xFF171f33),
    surfaceContainerHigh = Color(0xFF222a3d),
    surfaceContainerHighest = Color(0xFF2d3449),
    surfaceContainerLow = Color(0xFF131b2e),
    surfaceContainerLowest = Color(0xFF060e20),

    surfaceDim = Color(0xFF0b1326),
    surfaceBright = Color(0xFF31394d),

    outline = Color(0xFF8D9479),
    outlineVariant = Color(0xFF444748),

    inverseOnSurface = Color(0xFF283044),
    inverseSurface = Color(0xFFdae2fd),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000a),
    onErrorContainer = Color(0xFFffdad6)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFBEFF00),
    onPrimary = Color(0xFF253500),
    primaryContainer = Color(0xFFb8f600),
    onPrimaryContainer = Color(0xFF506e00),
    inversePrimary = Color(0xFF4b6700),

    secondary = Color(0xFFd2bbff),
    onSecondary = Color(0xFF38255e),
    secondaryContainer = Color(0xFF523e79),
    onSecondaryContainer = Color(0xFFc4adf0),

    tertiary = Color(0xFFffffff),
    onTertiary = Color(0xFF2f3131),
    tertiaryContainer = Color(0xFFe2e2e2),
    onTertiaryContainer = Color(0xFF636565),

    background = Color(0xFF0b1326),
    onBackground = Color(0xFFdae2fd),

    surface = Color(0xFF0B1326),
    onSurface = Color(0xFFdae2fd),
    surfaceVariant = Color(0xFF2d3449),
    onSurfaceVariant = Color(0xFFc4c7c8),

    surfaceContainer = Color(0xFF171f33),
    surfaceContainerHigh = Color(0xFF222a3d),
    surfaceContainerHighest = Color(0xFF2d3449),
    surfaceContainerLow = Color(0xFF131b2e),
    surfaceContainerLowest = Color(0xFF060e20),

    surfaceDim = Color(0xFF0b1326),
    surfaceBright = Color(0xFF31394d),

    outline = Color(0xFF8D9479),
    outlineVariant = Color(0xFF444748),

    inverseOnSurface = Color(0xFF283044),
    inverseSurface = Color(0xFFdae2fd),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000a),
    onErrorContainer = Color(0xFFffdad6)
)
