// app/src/main/java/com/sq/susurros/data/repository/LibroRepository.kt
package com.sq.susurros.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.sq.susurros.data.model.BookData
import javax.inject.Inject

/**
 * LibroRepository — Gestión de libros (PDF, ePub, audiolibros) via MediaStore y SAF.
 *
 * - Escaneo: usa MediaStore para encontrar PDF/EPUB/audio.
 * - Selección: usa Storage Access Framework (ACTION_OPEN_DOCUMENT).
 * - Persistencia: Room para metadatos del libro actual.
 */
class LibroRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {

    /**
     * Escanea el dispositivo en busca de libros (PDF, ePub, audiolibros).
     * Usa MediaStore con proyecciones específicas.
     */
    fun scanBooks(): List<BookData> {
        val books = mutableListOf<BookData>()

        // Escanear PDF
        val pdfProjection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val pdfSelection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val pdfArgs = arrayOf("application/pdf", "application/epub+zip")

        val collection = MediaStore.Files.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )

        contentResolver.query(
            collection,
            pdfProjection,
            pdfSelection,
            pdfArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val title = cursor.getString(nameIndex)
                val path = cursor.getString(dataIndex)
                val size = cursor.getLong(sizeIndex)
                val mime = cursor.getString(mimeIndex)

                val fileType = when (mime) {
                    "application/pdf" -> BookData.FileType.PDF
                    "application/epub+zip" -> BookData.FileType.EPUB
                    else -> BookData.FileType.AUDIOBOOK
                }

                val uri = Uri.withAppendedPath(collection, id.toString())

                books.add(
                    BookData(
                        id = id,
                        title = title,
                        author = null, // extraer de metadatos según tipo
                        uri = uri,
                        filePath = path,
                        fileSize = size,
                        durationMs = null,
                        fileType = fileType
                    )
                )
            }
        }

        return books
    }

    /**
     * Permite al usuario seleccionar un archivo vía SAF.
     * Devuelve el URI del archivo seleccionado.
     * (La Activity debe lanzar ACTION_OPEN_DOCUMENT y recibir el resultado.)
     */
    fun createOpenDocumentIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/epub+zip",
                "audio/*"
            ))
        }
        return intent
    }

    /**
     * Elimina un libro de la lista (marca como no-visble o elimina).
     */
    fun deleteBook(bookId: Long) {
        val collection = MediaStore.Files.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
        contentResolver.delete(
            Uri.withAppendedPath(collection, bookId.toString()),
            null,
            null
        )
    }
}
