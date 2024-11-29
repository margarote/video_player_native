package com.example.video_player_native

import android.net.Uri
import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player

@Composable
fun VideoPlayerComposable(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Estado para controlar o carregamento/bufferização
    var isBuffering by remember { mutableStateOf(true) }

    // Obter a instância do ExoPlayer
    val exoPlayer = remember {
        PlayerProvider.getPlayer(context).apply {
            // Configurar o MediaItem apenas se não estiver configurado
            if (currentMediaItem == null) {
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        }
    }

    // Adicionar um listener para atualizar o estado de bufferização
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    ExoPlayer.STATE_BUFFERING -> isBuffering = true
                    ExoPlayer.STATE_READY, ExoPlayer.STATE_ENDED, ExoPlayer.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Opcional: você pode atualizar isBuffering aqui se necessário
            }
        }

        exoPlayer.addListener(listener)

        // Remover o listener quando o Composable for descartado
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Integrar o PlayerView com o Compose e sobrepor o indicador de carregamento e botão de fechar
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Define o fundo como preto
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
        )

        if (isBuffering) {
            // Exibir um fundo semitransparente para destacar o indicador
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Botão para fechar a Activity
        IconButton(
            onClick = {
                val activity = context as? Activity
                activity?.finish()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(2f) // Certifique-se de que o botão esteja acima do PlayerView e do indicador de carregamento
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar",
                tint = Color.White
            )
        }
    }

    // Atualizar o MediaItem se o videoUrl mudar
    LaunchedEffect(videoUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Gerenciar o ciclo de vida do player
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.playWhenReady = true
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
