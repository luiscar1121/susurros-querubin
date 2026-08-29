// app/src/main/java/com/sq/susurros/ui/MainScreen.kt
package com.sq.susurros.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Pantalla principal del lector.
 * Placeholder inicial — será reemplazado por la composición completa
 * con ConstraintLayout, CircularDial, TimeSelector, PlaybackControls.
 */
@Composable
fun MainScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("SQ Susurros de Querubín")
    }
}
