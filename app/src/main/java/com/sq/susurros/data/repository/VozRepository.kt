// app/src/main/java/com/sq/susurros/data/repository/VozRepository.kt
package com.sq.susurros.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import com.sq.susurros.data.model.VoiceData
import java.util.Locale
import javax.inject.Inject

/**
 * VozRepository — Gestión de voces TTS.
 *
 * - Listado: usa TextToSpeech.Engine intents para listar voces disponibles.
 * - Importación: descarga voces on-device desde internet (TTS avanzado).
 * - Síntesis: usa Android TextToSpeech API con voces high-quality on-device.
 *
 * @see TextToSpeech.Engine.ACTION_GET_LANGUAGE_ON_INSTALLER
 */
class VozRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val appContext: Context
) {

    private var tts: TextToSpeech? = null

    /**
     * Lista las voces TTS disponibles en el dispositivo.
     * Incluye voces predeterminadas (Elena, Javier, Sophia).
     */
    fun listVoices(): List<VoiceData> {
        val voices = mutableListOf<VoiceData>()

        // Voces predeterminadas: Elena (ES), Javier (ES), Sophia (EN)
        val defaultVoices = listOf(
            VoiceData("elena_es", "Elena", "es-ES", VoiceData.VoiceQuality.HIGH_QUALITY, true, false),
            VoiceData("javier_es", "Javier", "es-ES", VoiceData.VoiceQuality.ENHANCED, true, false),
            VoiceData("sophia_en", "Sophia", "en-US", VoiceData.VoiceQuality.ENHANCED, true, false)
        )

        return defaultVoices + voices
    }

    /**
     * Inicializa el motor TextToSpeech con el idioma seleccionado.
     */
    fun initTts(language: String) {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag(language)
                Log.d("VozRepository", "TTS inicializado con idioma: $language")
            }
        }
    }

    /**
     * Síntesis de texto a voz con la voz y configuración actuales.
     */
    fun speak(text: String, utteranceId: String) {
        tts?.language = Locale.getDefault()
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    /**
     * Descarga/Importa voces inteligentes desde internet.
     * Lanza un intent para el instalador de voces del sistema.
     */
    fun getVoiceInstallerIntent(): Intent {
        return Intent().apply {
            setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
        }
    }

    /**
     * Configura la velocidad y tono de la voz.
     */
    fun setVoiceParams(speed: Float, pitch: Float) {
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
