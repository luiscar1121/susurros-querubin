// app/src/main/java/com/sq/susurros/data/model/VoiceData.kt
package com.sq.susurros.data.model

/**
 * Modelo de datos para una voz TTS.
 */
data class VoiceData(
    val id: String,
    val name: String,
    val locale: String,
    val quality: VoiceQuality,
    val isAvailable: Boolean,
    val requiresDownload: Boolean
) {
    enum class VoiceQuality {
        STANDARD,
        HIGH_QUALITY,
        ENHANCED
    }
}
