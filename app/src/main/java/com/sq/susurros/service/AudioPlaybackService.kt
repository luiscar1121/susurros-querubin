// app/src/main/java/com/sq/susurros/service/AudioPlaybackService.kt
package com.sq.susurros.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sq.susurros.R
import com.sq.susurros.data.repository.MusicaRepository
import com.sq.susurros.data.repository.VozRepository
import com.sq.susurros.domain.state.PlaybackState
import com.sq.susurros.domain.state.isPlaying
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class AudioPlaybackService : MediaSessionService() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "sq_audio_channel"
        const val CHANNEL_NAME = "SQ Susurros Audio"
        private const val TICK_INTERVAL_MS = 100L

        const val ACTION_START = "com.sq.susurros.START"
        const val ACTION_STOP = "com.sq.susurros.STOP"
        const val ACTION_PLAY_PAUSE = "com.sq.susurros.PLAY_PAUSE"
    }

    @Inject lateinit var stateManager: AudioStateManager
    @Inject lateinit var vozRepository: VozRepository
    @Inject lateinit var musicaRepository: MusicaRepository

    private var musicPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: NotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val tickerHandler = Handler(Looper.getMainLooper())
    private var tickerRunnable: Runnable? = null

    private var sentences: List<String> = emptyList()
    private var currentSentenceIndex: Int = 0
    private var isReading: Boolean = false

    override fun onCreate() {
        super.onCreate()
        initPlayers()
        initMediaSession()
        initNotificationChannel()
        initTts()
    }

    private fun initPlayers() {
        musicPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build(),
                true
            ).build()
        musicPlayer?.volume = 0.0f
    }

    private fun initMediaSession() {
        mediaSession = MediaSession.Builder(this, musicPlayer!!).build()
    }

    private fun initNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initTts() {
        vozRepository.initTts("es-ES") {
            // Callback cuando el TTS está listo
        }
        vozRepository.setOnSentenceFinishedListener {
            tickerHandler.post { onSentenceFinished() }
        }
        
        // Mock de contenido
        val bookMock = "La lectura inteligente del Lector 02 ya está activa. " +
                "Esta es la segunda frase del libro. " +
                "Esperamos que la música entre suavemente cuando el tiempo termine."
        sentences = bookMock.split(Regex("(?<=[.!?])\\s+"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundPlayback()
            ACTION_STOP -> stopForegroundAndSelf()
            ACTION_PLAY_PAUSE -> playPause()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundPlayback() {
        startForeground(
            NOTIFICATION_ID, 
            createNotification(), 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )

        serviceScope.launch {
            stateManager.state.onEach { state ->
                musicPlayer?.volume = state.musicVolume
                handleStateTransitions(state)
            }.collectLatest {}
        }

        startTicker()
    }

    private fun handleStateTransitions(state: PlaybackState) {
        when (state) {
            is PlaybackState.Listening -> {
                if (!isReading && state.isBookPlaying) startReading()
            }
            is PlaybackState.MusicFadeIn -> {
                if (!(musicPlayer?.isPlaying ?: false)) playRandomMusic()
            }
            is PlaybackState.Idle -> {
                isReading = false
                vozRepository.stop()
                musicPlayer?.pause()
            }
            is PlaybackState.BookResume -> {
                if (!isReading) startReading()
            }
            else -> {}
        }
    }

    private fun playRandomMusic() {
        val track = musicaRepository.getRandomTrack()
        if (track != null) {
            val mediaItem = MediaItem.fromUri(track.absolutePath)
            musicPlayer?.setMediaItem(mediaItem)
            musicPlayer?.prepare()
            musicPlayer?.play()
        }
    }

    private fun startReading() {
        if (isReading) return
        isReading = true
        readNextSentence()
    }

    private fun readNextSentence() {
        val state = stateManager.state.value
        if (!isReading || state is PlaybackState.Idle) return

        if (currentSentenceIndex < sentences.size) {
            val sentence = sentences[currentSentenceIndex]
            vozRepository.speak(sentence, "sentence_$currentSentenceIndex")
            currentSentenceIndex++
        } else {
            isReading = false
            stateManager.stop()
        }
    }

    private fun onSentenceFinished() {
        val state = stateManager.state.value
        // REGLA: Al agotarse el timer, detener lectura solo tras finalizar la frase actual.
        if (state is PlaybackState.Listening && state.remaining <= 0) {
            isReading = false
        } else if (isReading) {
            readNextSentence()
        }
    }

    private fun startTicker() {
        stopTicker()
        tickerRunnable = object : Runnable {
            override fun run() {
                stateManager.onTick(TICK_INTERVAL_MS)
                tickerHandler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
        tickerHandler.post(tickerRunnable!!)
    }

    private fun stopTicker() {
        tickerRunnable?.let { tickerHandler.removeCallbacks(it) }
    }

    private fun playPause() {
        val current = stateManager.state.value
        if (current.isPlaying) {
            stateManager.pause()
            isReading = false
            vozRepository.stop()
            musicPlayer?.pause()
        } else {
            stateManager.resume()
            if (current is PlaybackState.Listening) startReading()
            if (current is PlaybackState.MusicPlaying) musicPlayer?.play()
        }
    }

    private fun stopForegroundAndSelf() {
        stopTicker()
        stateManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SQ Susurros")
            .setContentText("Lector inteligente activo")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        stopTicker()
        serviceScope.cancel()
        mediaSession?.release()
        musicPlayer?.release()
        vozRepository.release()
        super.onDestroy()
    }
}
