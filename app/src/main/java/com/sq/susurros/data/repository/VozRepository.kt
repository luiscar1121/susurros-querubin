// app/src/main/java/com/sq/susurros/data/repository/VozRepository.kt
package com.sq.susurros.data.repository

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.sq.susurros.data.model.VoiceData
import java.util.Locale
import javax.inject.Inject

@Suppress("DEPRECATION")
class VozRepository @Inject constructor(
    private val appContext: Context
) {

    private var tts: TextToSpeech? = null
    private var onSentenceFinishedListener: (() -> Unit)? = null

    fun initTts(language: String, onReady: () -> Unit = {}) {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag(language)
                setupProgressListener()
                onReady()
                Log.d("VozRepository", "TTS inicializado con idioma: $language")
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { }
            override fun onDone(utteranceId: String?) {
                onSentenceFinishedListener?.invoke()
            }
            override fun onError(utteranceId: String?) { }
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
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun setVoiceParams(speed: Float, pitch: Float) {
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
