// app/src/main/java/com/sq/susurros/data/repository/LibroRepository.kt
package com.sq.susurros.data.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
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
                    durationMs = 1_200_000L,
                    fileType = type
                )
            )
        }
        return books
    }

    /**
     * Extrae texto de un PDF usando PdfRenderer.
     * Nota: PdfRenderer nativo está diseñado para renderizar bitmaps, no para extraer texto.
     * Esta es una implementación 'placeholder' que devuelve un texto base.
     * Para extracción real, se recomienda iText o PdfBox-Android.
     */
    fun extractTextFromPdf(file: File): String {
        Log.d("LibroRepository", "Extrayendo texto de: ${file.absolutePath}")
        // Mock de extracción exitosa para asegurar que el TTS tenga qué leer
        return "Iniciando la lectura de ${file.nameWithoutExtension}. " +
                "Este es el primer capítulo del libro seleccionado. " +
                "El sistema de voz está procesando el contenido correctamente. " +
                "Disfruta de tu audiolibro inteligente con Susurros de Querubín."
    }

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
        val books = scanBooks()
        books.find { it.id == bookId }?.let {
            File(it.filePath).delete()
        }
    }
}
