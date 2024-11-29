// VideoPlayerActivity.kt
package com.example.video_player_native

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class VideoPlayerActivity : ComponentActivity() {

    private lateinit var videoUrl: String

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

        setContent {
            VideoPlayerNativeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    VideoPlayerScreen(videoUrl = videoUrl)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar o player quando a Activity for destruída
        PlayerProvider.releasePlayer()
        Log.d("VideoPlayerActivity", "ExoPlayer liberado")
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

@Composable
fun VideoPlayerScreen(videoUrl: String) {
    VideoPlayerComposable(
        videoUrl = videoUrl,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    )
}