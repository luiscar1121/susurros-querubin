// app/src/main/java/com/sq/susurros/ui/theme/Theme.kt
package com.sq.susurros.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Light y Dark color schemes definidas en ColorScheme.kt (DarkColors / LightColors)
// Aquí se elige la apropiada según el modo del sistema.

@Composable
fun SQSusurrosTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = SQTypography(),
        content = content
    )
}

@Composable
fun SQTypography(): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 53.sp,
            letterSpacing = (-0.02).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 31.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 29.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        labelSmall = TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 12.sp
        ),
        labelMedium = TextStyle(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.05.sp
        )
    )
}
