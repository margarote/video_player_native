package com.example.video_player_native

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.analytics.AnalyticsListener
import io.flutter.plugin.common.MethodCall

@OptIn(UnstableApi::class)
class VideoPlayerView(
    context: Context,
    creationParams: Map<String, Any>?,
    messenger: MethodChannel
) : PlatformView, MethodChannel.MethodCallHandler {

    private val rootView: View
    private val playerView: PlayerView
    private val exoPlayer: ExoPlayer
    private val progressBar: ProgressBar
    private val fullscreenButton: ImageButton
    private val speedButton: ImageButton
    private val speedText: android.widget.TextView
    private var isFullscreen = false
    private var currentSpeed = 1.0f
    private val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    private var currentSpeedIndex = 3 // começa em 1.0x

    private val videoPlayerChannel: MethodChannel

    private val handler = Handler(Looper.getMainLooper())
    private val updateIntervalMs: Long = 1000 // 1 segundo
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            notifyTimeUpdate()
            handler.postDelayed(this, updateIntervalMs)
        }
    }

    init {
        val inflater = LayoutInflater.from(context)
        rootView = inflater.inflate(R.layout.activity_video_player_mini, null)

        // Configurar LayoutParams para ocupar todo o espaço
        rootView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        playerView = rootView.findViewById(R.id.player_view)
        progressBar = rootView.findViewById(R.id.progress_bar)
        fullscreenButton = rootView.findViewById(R.id.exo_fullscreen_button)
        speedButton = rootView.findViewById(R.id.exo_speed_button)
        speedText = rootView.findViewById(R.id.speed_text)

        val url = creationParams?.get("url") as? String ?: ""
        val durationInitial = creationParams?.get("duration_initial") as? Int ?: 0
        val initialSpeed = (creationParams?.get("playback_speed") as? Number)?.toFloat() ?: 1.0f

        if (url.isEmpty()) {
            messenger.invokeMethod("onError", "URL do vídeo está vazia")
        }

        // Inicializar o ExoPlayer com cache
        val cache = VideoCache.getInstance(context)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    .setUserAgent(Util.getUserAgent(context, context.packageName))
                    .setAllowCrossProtocolRedirects(true)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Configurar o PlayerView
        playerView.apply {
            player = exoPlayer
            setBackgroundColor(Color.BLACK)

            // Remover o botão de configurações
            val settingsButton = findViewById<View>(
                androidx.media3.ui.R.id.exo_settings
            )
            settingsButton?.visibility = View.GONE

            // Opcional: Remover o botão de legendas
            val subtitleButton = findViewById<View>(
                androidx.media3.ui.R.id.exo_subtitle
            )
            subtitleButton?.visibility = View.GONE
        }

        // Configurar o MediaItem
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer.playWhenReady = false
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Se tiver uma posição inicial, fazer seek quando o player estiver pronto
        if (durationInitial > 0) {
            val positionMs = (durationInitial * 1000).toLong()
            exoPlayer.seekTo(positionMs)
        }

        // Aplicar velocidade inicial se diferente de 1.0
        if (initialSpeed != 1.0f) {
            // Encontrar o índice correto no array de velocidades
            val index = speeds.indexOfFirst { it == initialSpeed }
            currentSpeedIndex = if (index >= 0) index else 3
            setPlaybackSpeed(initialSpeed)
        }

        // Listener para atualizar o estado de bufferização
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> progressBar.visibility = View.VISIBLE
                    Player.STATE_READY -> {
                        progressBar.visibility = View.GONE
                        startUpdatingTime()
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        progressBar.visibility = View.GONE
                        stopUpdatingTime()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                messenger.invokeMethod("onError", error.message)
                stopUpdatingTime()
            }
        })

        // Atualizar o tempo de reprodução a cada mudança significativa

        // Configurar o botão de tela cheia
        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        // Configurar o botão de velocidade
        speedButton.setOnClickListener {
            cycleSpeed()
        }

        // Configurar o MethodChannel para o VideoPlayerView
        videoPlayerChannel = messenger
        videoPlayerChannel.setMethodCallHandler(this)
    }

    private fun startUpdatingTime() {
        handler.post(updateTimeRunnable)
    }

    private fun stopUpdatingTime() {
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun notifyTimeUpdate() {
        if (exoPlayer.isPlaying) {
            val currentPositionMs = exoPlayer.currentPosition
            val currentPositionSeconds = currentPositionMs / 1000.0

            val durationMs = exoPlayer.duration
            val durationSeconds = if (durationMs > 0) durationMs / 1000.0 else 0.0

            println("Current Position: $currentPositionMs ms")
            println("Duration: $durationMs ms")

            // Cria um mapa com os dados a serem enviados ao Flutter
            val arguments = mapOf(
                "currentTime" to currentPositionSeconds,
                "duration" to durationSeconds
            )

            // Envia os dados para o Flutter
            videoPlayerChannel.invokeMethod(
                "onTimeUpdate",
                arguments
            )
        }
    }


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "setPlaybackPosition" -> {
                val seconds = call.argument<Double>("position")
                if (seconds != null) {
                    val positionMs = (seconds * 1000).toLong()
                    exoPlayer.seekTo(positionMs)
                    exoPlayer.play()
                    result.success(null)
                } else {
                    result.error("INVALID_ARGUMENT", "O parâmetro 'seconds' é necessário", null)
                }
            }
            "setPlaybackSpeed" -> {
                val speed = call.argument<Double>("speed")
                if (speed != null) {
                    setPlaybackSpeed(speed.toFloat())
                    result.success(null)
                } else {
                    result.error("INVALID_ARGUMENT", "O parâmetro 'speed' é necessário", null)
                }
            }
            "set_fullscreen" -> {
                val value = call.argument<Boolean>("value") ?: false
                isFullscreen = value
            }
            "onPipModeChanged" -> {
                val isInPip = call.argument<Boolean>("isInPip") ?: false
                handlePipChange(isInPip)
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        val playbackParameters = androidx.media3.common.PlaybackParameters(speed)
        exoPlayer.playbackParameters = playbackParameters
        currentSpeed = speed
        updateSpeedText(speed)
    }

    private fun updateSpeedText(speed: Float) {
        val speedLabel = when (speed) {
            0.25f -> "0.25x"
            0.5f -> "0.5x"
            0.75f -> "0.75x"
            1.0f -> "1x"
            1.25f -> "1.25x"
            1.5f -> "1.5x"
            1.75f -> "1.75x"
            2.0f -> "2x"
            else -> "${speed}x"
        }
        speedText.text = speedLabel
    }

    private fun cycleSpeed() {
        currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
        val newSpeed = speeds[currentSpeedIndex]
        setPlaybackSpeed(newSpeed)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen

        onFullscreenChange(isFullscreen)
    }

    fun onFullscreenChange(value: Boolean){
        val currentPositionMs = exoPlayer.currentPosition
        val currentPositionSeconds = currentPositionMs / 1000.0

        val fullscreenData = mapOf(
            "isFullscreen" to value,
            "currentPosition" to currentPositionSeconds,
            "playbackSpeed" to currentSpeed
        )

        videoPlayerChannel.invokeMethod("onFullscreenChange", fullscreenData)
    }

    /**
     * Método para reagir às mudanças do modo PiP
     */
    fun handlePipChange(isInPictureInPictureMode: Boolean) {
        playerView.useController = !isInPictureInPictureMode
    }

    override fun getView(): View = rootView

    override fun dispose() {
        stopUpdatingTime()
        exoPlayer.release()
    }

}
