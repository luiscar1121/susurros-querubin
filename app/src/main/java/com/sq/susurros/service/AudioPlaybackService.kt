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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sq.susurros.R
import com.sq.susurros.data.repository.LibroRepository
import com.sq.susurros.data.repository.MusicaRepository
import com.sq.susurros.data.repository.VozRepository
import com.sq.susurros.domain.state.PlaybackState
import com.sq.susurros.domain.state.isPlaying
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import java.io.File
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
        
        const val EXTRA_BOOK_PATH = "extra_book_path"
    }

    @Inject lateinit var stateManager: AudioStateManager
    @Inject lateinit var vozRepository: VozRepository
    @Inject lateinit var musicaRepository: MusicaRepository
    @Inject lateinit var libroRepository: LibroRepository

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
        Log.d("AudioService", "Servicio iniciado - onCreate")
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
            Log.d("AudioService", "TTS Motor Preparado")
            if (sentences.isNotEmpty() && isReading) {
                readNextSentence()
            }
        }
        vozRepository.setOnSentenceFinishedListener {
            tickerHandler.post { onSentenceFinished() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AudioService", "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                val bookPath = intent.getStringExtra(EXTRA_BOOK_PATH)
                if (bookPath != null) loadBookContent(bookPath)
                startForegroundPlayback()
            }
            ACTION_STOP -> stopForegroundAndSelf()
            ACTION_PLAY_PAUSE -> playPause()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun loadBookContent(path: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val text = libroRepository.extractTextFromPdf(file)
                    withContext(Dispatchers.Main) {
                        sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
                        currentSentenceIndex = 0
                        Log.d("AudioService", "Texto cargado: ${sentences.size} frases")
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioService", "Error cargando libro", e)
            }
        }
    }

    private fun startForegroundPlayback() {
        startForeground(
            NOTIFICATION_ID, 
            createNotification(), 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )

        serviceScope.launch {
            stateManager.state.collectLatest { state ->
                musicPlayer?.volume = state.musicVolume
                handleStateTransitions(state)
            }
        }

        startTicker()
    }

    private fun handleStateTransitions(state: PlaybackState) {
        when (state) {
            is PlaybackState.Listening -> {
                if (state.isBookPlaying && !isReading) startReading()
                else if (!state.isBookPlaying && isReading) pauseReading()
            }
            is PlaybackState.Idle -> {
                stopReading()
                musicPlayer?.pause()
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
        if (sentences.isEmpty()) return
        isReading = true
        readNextSentence()
    }

    private fun pauseReading() {
        isReading = false
        vozRepository.stop()
    }

    private fun stopReading() {
        isReading = false
        currentSentenceIndex = 0
        vozRepository.stop()
    }

    private fun readNextSentence() {
        if (!isReading || stateManager.state.value is PlaybackState.Idle) return

        if (currentSentenceIndex < sentences.size) {
            val sentence = sentences[currentSentenceIndex]
            vozRepository.speak(sentence, "sentence_$currentSentenceIndex")
            currentSentenceIndex++
        } else {
            stopReading()
            stateManager.stop()
        }
    }

    private fun onSentenceFinished() {
        val state = stateManager.state.value
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
        // La lógica real ahora la maneja el stateManager, nosotros solo reaccionamos al Flow en handleStateTransitions
    }

    private fun stopForegroundAndSelf() {
        stopTicker()
        stopReading()
        stateManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SQ Susurros")
            .setContentText("Reproducción activa")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        Log.d("AudioService", "Service onDestroy")
        stopTicker()
        serviceScope.cancel()
        mediaSession?.release()
        musicPlayer?.release()
        vozRepository.release()
        super.onDestroy()
    }
}
