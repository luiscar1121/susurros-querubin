// app/src/main/java/com/sq/susurros/data/repository/LibroRepository.kt
package com.sq.susurros.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sq.susurros.data.model.BookData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * LibroRepository — Gestión de libros (PDF, ePub) para Lector 02.
 */
class LibroRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    @ApplicationContext private val context: Context
) {

    private val booksDir: File by lazy {
        File(context.getExternalFilesDir(null), "Libros").apply { mkdirs() }
    }

    /**
     * Escanea la carpeta interna de la app en busca de libros guardados.
     */
    fun scanBooks(): List<BookData> {
        val books = mutableListOf<BookData>()
        val files = booksDir.listFiles() ?: return emptyList()

        files.forEachIndexed { index, file ->
            val type = when (file.extension.lowercase()) {
                "pdf" -> BookData.FileType.PDF
                "epub" -> BookData.FileType.EPUB
                else -> BookData.FileType.AUDIOBOOK
            }
            books.add(
                BookData(
                    id = index.toLong(),
                    title = file.nameWithoutExtension,
                    author = "Autor Desconocido",
                    uri = Uri.fromFile(file),
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    durationMs = 1_200_000L, // 20m default
                    fileType = type
                )
            )
        }
        return books
    }

    /**
     * Guarda un libro seleccionado vía SAF en la carpeta interna.
     */
    fun saveBookToInternal(uri: Uri): BookData? {
        return try {
            val fileName = getFileName(uri) ?: "libro_${System.currentTimeMillis()}.pdf"
            val destFile = File(booksDir, fileName)
            
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            BookData(
                id = System.currentTimeMillis(),
                title = destFile.nameWithoutExtension,
                author = "Importado",
                uri = Uri.fromFile(destFile),
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                durationMs = 1_200_000L,
                fileType = if (destFile.extension.lowercase() == "epub") BookData.FileType.EPUB else BookData.FileType.PDF
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }

    fun deleteBook(bookId: Long) {
        // En esta versión simple, borrar por ID requiere mapeo o borrar por archivo
        val books = scanBooks()
        books.find { it.id == bookId }?.let {
            File(it.filePath).delete()
        }
    }
}
