package com.example.video_player_native

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoUrl: String
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: ImageButton

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("VideoPlayerActivity", "onCreate chamado")

        // Obter a URL do vídeo via Intent
        videoUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        Log.d("VideoPlayerActivity", "URL do vídeo: $videoUrl")

        if (videoUrl.isEmpty()) {
            Log.e("VideoPlayerActivity", "URL do vídeo está vazia")
            finish()
            return
        }

        // Definir o layout da atividade
        setContentView(R.layout.activity_video_player)

        // Inicializar as views
        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progress_bar)
        closeButton = findViewById(R.id.close_button)

        // Inicializar o ExoPlayer com cache
        initExoPlayerWithCache(this)

        // Configurar o botão de fechar
        closeButton.setOnClickListener {
            finish()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun initExoPlayerWithCache(context: Context) {
        val cache = VideoCache.getInstance(context)

        // Salvar metadados de cache (opcional)
        val uri = Uri.parse(videoUrl)
        VideoCache.saveCacheMetadata(context, uri)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent(Util.getUserAgent(context, context.packageName))
                    .setAllowCrossProtocolRedirects(true)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Criar a MediaSourceFactory com o DataSource.Factory de cache
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        // Inicializar o ExoPlayer
        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Configurar o PlayerView
        playerView.apply {
            player = exoPlayer
            setBackgroundColor(Color.BLACK)
        }

        // Configurar o MediaItem
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // Listener para atualizar o estado de bufferização
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> progressBar.visibility = View.VISIBLE
                    Player.STATE_READY, Player.STATE_ENDED, Player.STATE_IDLE -> progressBar.visibility = View.GONE
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        Log.d("VideoPlayerActivity", "ExoPlayer liberado")
        VideoCache.release()
    }

    companion object {
        fun start(context: Context, videoUrl: String) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra("VIDEO_URL", videoUrl)
            }
            context.startActivity(intent)
            Log.d("VideoPlayerActivity", "Atividade iniciada com URL: $videoUrl")
        }
    }
}
