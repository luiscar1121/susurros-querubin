// app/src/main/java/com/sq/susurros/data/repository/MusicaRepository.kt
package com.sq.susurros.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.sq.susurros.data.model.MusicData
import java.io.File
import javax.inject.Inject

/**
 * MusicaRepository — Gestión de música de fondo.
 *
 * - Escaneo: usa MediaStore con READ_MEDIA_AUDIO para encontrar MP3/M4A.
 * - Almacenamiento interno: guarda selecciones en getExternalFilesDir("Música").
 * - Reproducción: selección aleatoria (random) desde la carpeta interna.
 */
class MusicaRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val appContext: android.content.Context
) {

    private val musicDir: File by lazy {
        File(appContext.getExternalFilesDir(null), "Música").apply { mkdirs() }
    }

    /**
     * Escanea el dispositivo en busca de archivos de música (MP3, M4A).
     * Requiere permiso READ_MEDIA_AUDIO.
     */
    fun scanMusic(): List<MusicData> {
        val tracks = mutableListOf<MusicData>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val title = cursor.getString(titleIndex)
                val artist = cursor.getString(artistIndex)
                val album = cursor.getString(albumIndex)
                val path = cursor.getString(dataIndex)
                val size = cursor.getLong(sizeIndex)
                val duration = cursor.getLong(durationIndex)

                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                tracks.add(
                    MusicData(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uri,
                        filePath = path,
                        durationMs = duration,
                        fileSize = size
                    )
                )
            }
        }

        return tracks
    }

    /**
     * Guarda un archivo de música seleccionado en la carpeta interna "Música".
     * Retorna el path del archivo guardado, o null si falla.
     */
    fun saveMusicToInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "track_${System.currentTimeMillis()}.mp3"
            val destFile = File(musicDir, fileName)
            destFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene una lista de las pistas guardadas en la carpeta interna.
     * Se usarán para la selección aleatoria (random).
     */
    fun getInternalMusicTracks(): List<File> {
        return if (musicDir.exists()) {
            musicDir.listFiles { file ->
                file.isFile && (
                    file.extension.equals("mp3", ignoreCase = true) ||
                    file.extension.equals("m4a", ignoreCase = true)
                )
            }?.toList() ?: emptyList()
        } else emptyList()
    }

    /**
     * Selecciona una pista aleatoria de la carpeta interna "Música".
     */
    fun getRandomTrack(): File? {
        val tracks = getInternalMusicTracks()
        return if (tracks.isNotEmpty()) {
            tracks.random()
        } else null
    }

    /**
     * Permite eliminar una pista de la lista interna.
     */
    fun deleteInternalTrack(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
}
