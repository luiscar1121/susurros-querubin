// app/src/main/java/com/sq/susurros/data/model/BookData.kt
package com.sq.susurros.data.model

import android.net.Uri

/**
 * Modelo de datos para un libro.
 */
data class BookData(
    val id: Long,
    val title: String,
    val author: String?,
    val uri: Uri,
    val filePath: String,
    val fileSize: Long,
    val durationMs: Long?,
    val fileType: FileType
) {
    enum class FileType {
        PDF,
        EPUB,
        AUDIOBOOK
    }
}
