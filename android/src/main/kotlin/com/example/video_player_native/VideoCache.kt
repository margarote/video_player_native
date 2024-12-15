package com.example.video_player_native

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File


object VideoCache {
    @UnstableApi
    @Volatile
    private var cache: SimpleCache? = null
    private const val CACHE_METADATA_PREFS = "VideoCachePrefs"
    private const val CACHE_SENT_URLS = "SentURLs"
    private const val CACHE_EXPIRY_IN_SECONDS: Long = 3600 * 24 * 10 // 10 dias
    private const val TAG = "VideoCache"


    private fun sharedPreferences(context: Context) : SharedPreferences {
        return context.getSharedPreferences("SentURLsPrefs", Context.MODE_PRIVATE)
    }


    fun GetSentURLs(context: Context): MutableSet<String> {
        // Retorna o conjunto de URLs armazenadas
        val prefs = sharedPreferences(context)
        val urls = prefs.getStringSet(CACHE_SENT_URLS, mutableSetOf())
        return urls ?: mutableSetOf()
    }

    // Marca uma URL como enviada
    fun markURLAsSent(url: String, context: Context) {
        // Trabalhar diretamente no SharedPreferences
        val sharedPreferences = sharedPreferences(context)
        val editor = sharedPreferences.edit()
        val urls = GetSentURLs(context)

        if (!urls.contains(url)) {
            // Atualiza o conjunto e salva
            urls.add(url)
            editor.putStringSet(CACHE_SENT_URLS, urls)
            editor.apply()
            println("URL marcada como enviada: $url")
        } else {
            println("URL já marcada como enviada: $url")
        }
    }

    // Verifica se uma URL já foi marcada como enviada
    fun IsURLMarkedAsSent(url: String, context: Context): Boolean {
        val urls = GetSentURLs(context)

        return  urls.contains(url)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun getInstance(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: createCache(context).also { cache = it }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun createCache(context: Context): SimpleCache {
        val desiredCacheSize: Long = 5L * 1024 * 1024 * 1024 // 5 GB
        val availableStorage = getAvailableStorage()

        val cacheSize = if (availableStorage > desiredCacheSize) {
            desiredCacheSize
        } else {
            availableStorage / 2 // Usar metade do espaço disponível
        }

        return try {
            SimpleCache(
                File(context.cacheDir, "VideoCache"),
                LeastRecentlyUsedCacheEvictor(cacheSize),
                ExoDatabaseProvider(context)
            ).also {
                cleanExpiredCache(context, it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar SimpleCache", e)
            throw e
        }
    }

    private fun getAvailableStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBlocks: Long = stat.availableBlocksLong
        val blockSize: Long = stat.blockSizeLong
        return availableBlocks * blockSize
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun cleanExpiredCache(context: Context, cache: SimpleCache) {
        val sentURLsPrefs = sharedPreferences(context)
        val sentUrlsEditor = sentURLsPrefs.edit()
        val prefs = context.getSharedPreferences(CACHE_METADATA_PREFS, Context.MODE_PRIVATE)
        val metadata = prefs.all
        val editor = prefs.edit()
        val currentTime = System.currentTimeMillis() / 1000

        for ((key, value) in metadata) {
            if (value is Long) {
                if (currentTime - value > CACHE_EXPIRY_IN_SECONDS) {
                    try {
                        val uri = Uri.parse(key)
                        cache.removeResource(uri.toString())
                        editor.remove(key)

                        // Remover também do SentURLsPrefs
                        val urlToRemove = uri.toString()
                        if (sentURLsPrefs.contains(urlToRemove)) {
                            sentUrlsEditor.remove(urlToRemove)
                            sentUrlsEditor.apply()
                            Log.d(TAG, "URL removida de SentURLsPrefs: $urlToRemove")
                        }
                        Log.d(TAG, "Removido do cache: $key")
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao remover recurso do cache: $key", e)
                    }
                }
            }
        }

        editor.apply()
    }

    fun saveCacheMetadata(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(CACHE_METADATA_PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val currentTime = System.currentTimeMillis() / 1000
        editor.putLong(uri.toString(), currentTime)
        editor.apply()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun release(context: Context) {
        try {
            cache?.release()
            cache = null
            Log.d(TAG, "Cache e SentURLsPrefs liberados")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar o cache", e)
        }
    }
}
