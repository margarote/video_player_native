package com.example.video_player_native

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

object PlayerProvider {
    private var exoPlayer: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    fun getPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val cache: Cache? = VideoCache.getInstance(context)

            val uri = Uri.parse("mediaUrl")

            // Salva metadados de cache
            VideoCache.saveCacheMetadata(context, uri)

            val dataSourceFactory: DataSource.Factory = if (cache != null) {
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory().setUserAgent(Util.getUserAgent(context, context.packageName)).setAllowCrossProtocolRedirects(true))
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } else {
                DefaultDataSourceFactory(
                    context,
                    Util.getUserAgent(context, context.packageName)
                )
            }

            // Criar a MediaSourceFactory com o DataSource.Factory de cache
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    playWhenReady = true
                    // Exemplo de como configurar o MediaSource com cache
                    // Substitua "mediaUrl" pela URL do seu vídeo
                    val mediaItem = MediaItem.fromUri("mediaUrl")
                    setMediaItem(mediaItem)
                    prepare()
                }
        }
        return exoPlayer!!
    }


    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
        VideoCache.release()
    }
}
