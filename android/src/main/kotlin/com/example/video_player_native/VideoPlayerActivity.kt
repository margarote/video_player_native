package com.example.video_player_native

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.ui.PlayerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: ImageButton
    private lateinit var speedButton: ImageButton
    private lateinit var speedText: android.widget.TextView

    private var currentSpeed = 1.0f
    private val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    private var currentSpeedIndex = 3 // começa em 1.0x

    private lateinit var videoUrl: String
    private lateinit var momentaryId: String
    private lateinit var fileId: String
    private lateinit var userId: String


    private var isVideoMarkedAsComplete = false
    private val apiBaseURL = "https://revivamomentos.com"
    // Calcula os bytes transferidos
    var totalBytesConsumed = 0L
    var totalBytesDownloaded = 0L

    // Flag para detectar se está mudando de orientação
    private var isChangingConfiguration = false

    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            // Verificar progresso do vídeo
            checkVideoProgress()
            // Continuar executando a cada 1 segundo
            handler.postDelayed(this, 1000)
        }
    }



    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("VideoPlayerActivity", "onCreate chamado")

        // Obter a URL do vídeo via Intent
        videoUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        momentaryId = intent.getStringExtra("EXTRA_MOMENTARY_ID") ?: ""
        fileId = intent.getStringExtra("EXTRA_FILE_ID") ?: ""
        userId = intent.getStringExtra("EXTRA_USER_ID") ?: ""

        // Iniciar o monitoramento de progresso
        handler.post(progressUpdateRunnable)

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
        speedButton = findViewById(R.id.speed_button)
        speedText = findViewById(R.id.speed_text)

        // Inicializar o ExoPlayer com cache
        initExoPlayerWithCache(this)

        // Configurar o botão de fechar
        closeButton.setOnClickListener {
            finish()
        }

        // Configurar o botão de velocidade
        speedButton.setOnClickListener {
            cycleSpeed()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun initExoPlayerWithCache(context: Context) {
        val cache = VideoCache.getInstance(context)

        val videoFile = File(videoUrl)
        val existFile = videoFile.exists()
        if (!existFile) {
            Log.e("VideoPlayerActivity", "Arquivo de vídeo não encontrado: $videoUrl")
            finish()
            return
        }

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
        exoPlayer = if(existFile){
            ExoPlayer.Builder(context)
                .build()
        } else {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        }

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

        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData
            ) {
                // Atualizar os bytes transferidos
                println("Bytes carregados nesta chamada: ${loadEventInfo.bytesLoaded}")

                // Enviar os dados de uso para a API
                SendDataUsage(context, loadEventInfo.bytesLoaded, momentaryId, fileId, userId, videoUrl)
            }
        })
    }

    private fun checkVideoProgress() {
        val currentPosition = exoPlayer.currentPosition.toDouble()
        val totalDuration = exoPlayer.duration.toDouble()

        if (!isVideoMarkedAsComplete && totalDuration > 0) {
            val progressPercentage = (currentPosition / totalDuration) * 100
            println("Progresso do vídeo: $progressPercentage%")

            if (progressPercentage >= 70) {
                println("Vídeo atingiu 70% de progresso. Marcando como concluído.")
                VideoCache.markURLAsSent(videoUrl, this) // Marcar como concluído
                isVideoMarkedAsComplete = true
            }
        }
    }


    fun SendDataUsage(
        context: Context,
        newValue: Long,
        momentaryId: String,
        fileId: String,
        userId: String,
        videoURL: String
    ) {
        // Verificar se a URL já foi marcada como enviada
        if (VideoCache.IsURLMarkedAsSent(videoURL, context)) {
            println("URL já marcada como enviada: $videoURL")
            return
        }

        if (newValue <= totalBytesConsumed) {
            println("Nenhum dado novo baixado.")
            return
        }

        totalBytesDownloaded = newValue - totalBytesConsumed
        totalBytesConsumed = newValue

        if (totalBytesDownloaded <= 0) {
            println("Nenhum dado baixado da rede. Nenhum envio necessário.")
            return
        }

        // Configurar a URL da API
        val apiURL = "$apiBaseURL/revivamomentos/bandwidth/create/usage"

        // Criar o corpo da requisição
        val requestBody = JSONObject().apply {
            put("momentary_id", momentaryId)
            put("file_id", fileId)
            put("user_id", userId)
            put("url", videoURL)
            put("type_file", "video")
            put("bytes_downloaded", totalBytesDownloaded)
        }

        // Criar a requisição HTTP
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(apiURL)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody.toString()))
            .build()

        // Enviar a requisição
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Erro ao enviar dados: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("Resposta da API: ${response.code}")
                } else {
                    println("Erro na resposta da API: ${response.code}")
                }
                response.close()
            }
        })
    }

    /**
     * Método para definir a velocidade de reprodução
     */
    fun setPlaybackSpeed(speed: Float) {
        val playbackParameters = androidx.media3.common.PlaybackParameters(speed)
        exoPlayer.playbackParameters = playbackParameters
        currentSpeed = speed
        updateSpeedText(speed)
        Log.d("VideoPlayerActivity", "Velocidade de reprodução alterada para: $speed")
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

    override fun onPause() {
        super.onPause()
        // Só pausa se NÃO estiver mudando de configuração (rotação)
        if (!isChangingConfigurations) {
            exoPlayer.pause()
            Log.d("VideoPlayerActivity", "Player pausado (não é rotação)")
        } else {
            Log.d("VideoPlayerActivity", "onPause durante rotação - player continua")
        }
    }

    override fun onResume() {
        super.onResume()
        // Só retoma se NÃO estiver mudando de configuração
        if (!isChangingConfigurations) {
            exoPlayer.playWhenReady = true
            Log.d("VideoPlayerActivity", "Player retomado")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        handler.removeCallbacks(progressUpdateRunnable)
        Log.d("VideoPlayerActivity", "ExoPlayer liberado")
        VideoCache.release(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("VideoPlayerActivity", "Orientação mudou")

        // Salvar estado de reprodução antes da mudança
        val wasPlaying = exoPlayer.isPlaying
        val currentPosition = exoPlayer.currentPosition

        // Trata mudanças de orientação da tela
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Log.d("VideoPlayerActivity", "Modo paisagem")
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                Log.d("VideoPlayerActivity", "Modo retrato")
            }
        }

        // Garantir que o player continue no mesmo estado
        if (wasPlaying) {
            // Pequeno delay para garantir que o layout foi ajustado
            playerView.postDelayed({
                if (!exoPlayer.isPlaying) {
                    exoPlayer.seekTo(currentPosition)
                    exoPlayer.play()
                    Log.d("VideoPlayerActivity", "Player retomado após rotação")
                }
            }, 100)
        }
    }

    companion object {
        private val VIDEO_URL = "VIDEO_URL"
        private val EXTRA_MOMENTARY_ID = "EXTRA_MOMENTARY_ID"
        private val EXTRA_FILE_ID = "EXTRA_FILE_ID"
        private val EXTRA_USER_ID = "EXTRA_USER_ID"

        fun start(context: Context, videoUrl: String,
                  momentaryId: String,
                  fileId: String,
                  userId: String,
                  ) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(VIDEO_URL, videoUrl)
                putExtra(EXTRA_MOMENTARY_ID, momentaryId)
                putExtra(EXTRA_FILE_ID, fileId)
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startActivity(intent)
            Log.d("VideoPlayerActivity", "Atividade iniciada com URL: $videoUrl")
        }
    }
}
