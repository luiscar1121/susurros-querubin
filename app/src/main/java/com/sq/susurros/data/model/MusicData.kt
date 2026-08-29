// app/src/main/java/com/sq/susurros/data/model/MusicData.kt
package com.sq.susurros.data.model

import android.net.Uri

/**
 * Modelo de datos para un archivo de música.
 */
data class MusicData(
    val id: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val uri: Uri,
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long
)
