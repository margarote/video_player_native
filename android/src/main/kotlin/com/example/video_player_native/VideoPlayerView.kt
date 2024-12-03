package com.example.video_player_native

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
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
import io.flutter.plugin.common.MethodCall

@OptIn(UnstableApi::class)
class VideoPlayerView(
    context: Context,
    id: Int,
    creationParams: Map<String, Any>?,
    messenger: MethodChannel
) : PlatformView, MethodChannel.MethodCallHandler {

    private val rootView: View
    private val playerView: PlayerView
    private val exoPlayer: ExoPlayer
    private val progressBar: ProgressBar
    private val fullscreenButton: ImageButton
    private var isFullscreen = false

    private val videoPlayerChannel: MethodChannel

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

        val url = creationParams?.get("url") as? String ?: ""
        val durationInitial = creationParams?.get("duration_initial") as? Int ?: 0

        if (url.isEmpty()) {
            // Tratar URL vazia
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
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        val positionMs = (durationInitial * 1000).toLong()
        exoPlayer.seekTo(positionMs)

        // Listener para atualizar o estado de bufferização
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> progressBar.visibility = View.VISIBLE
                    Player.STATE_READY, Player.STATE_ENDED, Player.STATE_IDLE -> progressBar.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                messenger.invokeMethod("onError", error.message)
            }
        })

        // Configurar o botão de tela cheia
        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        // Configurar o MethodChannel para o VideoPlayerView
        videoPlayerChannel = messenger
        videoPlayerChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "seekTo" -> {
                val seconds = call.argument<Double>("seconds")
                if (seconds != null) {
                    val positionMs = (seconds * 1000).toLong()
                    exoPlayer.seekTo(positionMs)
                    result.success(null)
                } else {
                    result.error("INVALID_ARGUMENT", "O parâmetro 'seconds' é necessário", null)
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

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen

        onFullscreenChange(isFullscreen)
    }

    fun onFullscreenChange(value: Boolean){
        val currentPositionMs = exoPlayer.currentPosition
        val currentPositionSeconds = currentPositionMs / 1000.0

        val fullscreenData = mapOf(
            "isFullscreen" to value,
            "currentPosition" to currentPositionSeconds
        )

        videoPlayerChannel.invokeMethod("onFullscreenChange", fullscreenData)
    }

    /**
     * Método para reagir às mudanças do modo PiP
     */
    fun handlePipChange(isInPictureInPictureMode: Boolean) {
        playerView.useController = !isInPictureInPictureMode
        onFullscreenChange(false)
    }

    override fun getView(): View = rootView

    override fun dispose() {
        exoPlayer.release()
    }
}
