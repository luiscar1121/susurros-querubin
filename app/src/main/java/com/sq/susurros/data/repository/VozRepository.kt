// app/src/main/java/com/sq/susurros/data/repository/VozRepository.kt
package com.sq.susurros.data.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.sq.susurros.data.model.VoiceData
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VozRepository @Inject constructor(
    private val appContext: Context
) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var onSentenceFinishedListener: (() -> Unit)? = null

    fun initTts(language: String, onReady: () -> Unit = {}) {
        if (tts != null) {
            if (isTtsReady) onReady()
            return
        }

        Log.d("VozRepository", "Inicializando motor TTS...")
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.forLanguageTag(language))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("VozRepository", "Idioma no soportado: $language")
                } else {
                    isTtsReady = true
                    setupProgressListener()
                    onReady()
                    Log.d("VozRepository", "TTS listo y configurado para: $language")
                }
            } else {
                Log.e("VozRepository", "Fallo al inicializar TTS. Status: $status")
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("VozRepository", "Empezando locución: $utteranceId")
            }
            override fun onDone(utteranceId: String?) {
                Log.d("VozRepository", "Locución terminada: $utteranceId")
                onSentenceFinishedListener?.invoke()
            }
            override fun onError(utteranceId: String?) {
                Log.e("VozRepository", "Error en locución: $utteranceId")
            }
        })
    }

    fun setOnSentenceFinishedListener(listener: () -> Unit) {
        this.onSentenceFinishedListener = listener
    }

    fun listVoices(): List<VoiceData> {
        return listOf(
            VoiceData("elena_es", "Elena", "es-ES", VoiceData.VoiceQuality.HIGH_QUALITY, true, false),
            VoiceData("javier_es", "Javier", "es-ES", VoiceData.VoiceQuality.ENHANCED, true, false),
            VoiceData("sophia_en", "Sophia", "en-US", VoiceData.VoiceQuality.ENHANCED, true, false)
        )
    }

    fun speak(text: String, utteranceId: String) {
        if (!isTtsReady || tts == null) {
            Log.w("VozRepository", "speak() llamado pero TTS no está listo")
            return
        }
        Log.d("VozRepository", "TTS Hablando: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun setVoiceParams(speed: Float, pitch: Float) {
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
    }

    fun stop() {
        Log.d("VozRepository", "Deteniendo locución TTS")
        tts?.stop()
    }

    fun release() {
        Log.d("VozRepository", "Liberando recursos TTS")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }
}
