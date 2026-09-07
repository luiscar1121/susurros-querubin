// app/src/main/java/com/sq/susurros/service/AudioPlaybackService.kt
package com.sq.susurros.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sq.susurros.R
import com.sq.susurros.domain.state.PlaybackState
import com.sq.susurros.domain.state.isPlaying
import com.sq.susurros.domain.state.bookVolume
import com.sq.susurros.domain.state.musicVolume
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AudioPlaybackService — Foreground Service con Media3.
 *
 * Administra dos reproductores:
 * 1. mediaPlayer — TTS/libro (audio)
 * 2. musicPlayer — música de fondo
 *
 * El AudioStateManager orquesta los fades y crossfades entre ambos.
 */
@AndroidEntryPoint
@UnstableApi
class AudioPlaybackService : MediaSessionService() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "sq_audio_channel"
        const val CHANNEL_NAME = "SQ Susurros Audio"
        private const val TICK_INTERVAL_MS = 100L // tick de timing para fades

        // Acciones del servicio
        const val ACTION_START = "com.sq.susurros.START"
        const val ACTION_STOP = "com.sq.susurros.STOP"
        const val ACTION_PLAY_PAUSE = "com.sq.susurros.PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.sq.susurros.SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.sq.susurros.SKIP_PREVIOUS"
    }

    @Inject
    lateinit var stateManager: AudioStateManager

    private var mediaPlayer: ExoPlayer? = null
    private var musicPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: NotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + Job())
    private val tickerHandler = Handler(Looper.getMainLooper())
    private var tickerRunnable: Runnable? = null

    // Callback para recibir comandos desde el ViewModel/Activity
    private var onStateChange: ((PlaybackState) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        initPlayers()
        initMediaSession()
        initNotificationChannel()
    }

    private fun initPlayers() {
        val audioAttrs = android.media.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        // Reproductor principal (TTS/libro)
        mediaPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()

        // Reproductor de música de fondo
        musicPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()

        // Ajustar volúmenes iniciales
        mediaPlayer?.volume = 1.0f
        musicPlayer?.volume = 0.0f
    }

    private fun initMediaSession() {
        mediaSession = MediaSession.Builder(this, mediaPlayer!!)
            .setFlags(MediaSession.FLAG_HANDLED_MEDIA_ACTIONS)
            .build()
        // Set the session to handle media commands
        mediaSession?.setMediaButtonReceiver(null)
    }

    private fun initNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Control de reproducción de SQ Susurros"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundPlayback()
            ACTION_STOP -> stopForegroundAndSelf()
            ACTION_PLAY_PAUSE -> playPause()
            ACTION_SKIP_NEXT -> skipForward()
            ACTION_SKIP_PREVIOUS -> skipBackward()
        }
        return START_STICKY
    }

    private fun startForegroundPlayback() {
        // Notificación con acciones directas
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )

        // Observar el estado del AudioStateManager y aplicar volúmenes/acciones
        serviceScope.launch {
            stateManager.state.onEach { state ->
                // Aplicar volúmenes a los MediaPlayers
                mediaPlayer?.volume = state.bookVolume
                musicPlayer?.volume = state.musicVolume
                // Notificar a la UI
                onStateChange?.invoke(state)
            }.collectLatest {}
        }

        // Iniciar ticker para actualizar estados de fades
        startTicker()
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
        val currentState = stateManager.state.value
        if (currentState is PlaybackState.Listening && currentState.isBookPlaying) {
            stateManager.pause()
            mediaPlayer?.pause()
        } else {
            stateManager.resume()
            mediaPlayer?.play()
        }
    }

    private fun skipForward() {
        mediaPlayer?.seekTo((mediaPlayer?.currentPosition ?: 0L) + 10_000L)
    }

    private fun skipBackward() {
        mediaPlayer?.seekTo((mediaPlayer?.currentPosition ?: 0L) - 10_000L)
    }

    private fun stopForegroundAndSelf() {
        stopTicker()
        stateManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Notificación con acciones: Play/Pause, Skip Next, Skip Previous.
     * Permite controlar la reproducción directamente desde la barra de notificaciones.
     */
    private fun createNotification(): Notification {
        val actionStop = android.app.PendingIntent.getService(
            this, 0, Intent(this, AudioPlaybackService::class.java).setAction(ACTION_STOP),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val isPlaying = stateManager.state.value.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseText = if (isPlaying) "Pausar" else "Reproducir"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SQ Susurros de Querubín")
            .setContentText("Reproduciendo: Crónica de una Muerte Anunciada")
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(
                R.drawable.ic_skip_previous,
                "Anterior",
                android.app.PendingIntent.getService(
                    this, 2, Intent(this, AudioPlaybackService::class.java)
                        .setAction(ACTION_SKIP_PREVIOUS),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                playPauseIcon,
                playPauseText,
                android.app.PendingIntent.getService(
                    this, 1, Intent(this, AudioPlaybackService::class.java)
                        .setAction(ACTION_PLAY_PAUSE),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                R.drawable.ic_skip_next,
                "Siguiente",
                android.app.PendingIntent.getService(
                    this, 3, Intent(this, AudioPlaybackService::class.java)
                        .setAction(ACTION_SKIP_NEXT),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()
    }

    /**
     * Prepara el reproductor del libro con el contenido TTS/audio.
     * Se llama cuando se selecciona un libro desde la app.
     */
    fun prepareBook(uriString: String, timerMs: Long) {
        val mediaItem = MediaItem.fromUri(uriString)
        mediaPlayer?.setMediaItem(mediaItem)
        mediaPlayer?.prepare()
        stateManager.startListening(bookPosition = 0L, timerMs = timerMs)
    }

    /**
     * Prepara la música de fondo.
     */
    fun prepareMusic(uriString: String, durationMs: Long) {
        val mediaItem = MediaItem.fromUri(uriString)
        musicPlayer?.setMediaItem(mediaItem)
        musicPlayer?.prepare()
    }

    fun setOnStateChangeListener(listener: (PlaybackState) -> Unit) {
        this.onStateChange = listener
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicker()
        serviceScope.cancel()
        mediaSession?.release()
        mediaPlayer?.release()
        musicPlayer?.release()
        super.onDestroy()
    }
}