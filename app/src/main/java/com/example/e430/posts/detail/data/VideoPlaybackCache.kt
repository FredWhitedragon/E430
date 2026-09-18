package com.example.e430.posts.detail.data

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.e430.core.network.E430_USER_AGENT
import java.io.File

/** Process-wide video segment cache. Files follow the same app-cache lifetime as image files. */
@UnstableApi
object VideoPlaybackCache {
    @Volatile
    private var cache: SimpleCache? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        if (cache == null) synchronized(this) {
            if (cache == null) cache = createCache(appContext)
        }
    }

    fun dataSourceFactory(context: Context): DataSource.Factory {
        val upstream = DefaultHttpDataSource.Factory().setUserAgent(E430_USER_AGENT)
        val sharedCache = cache ?: return upstream
        return CacheDataSource.Factory()
            .setCache(sharedCache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun createCache(context: Context): SimpleCache {
        val cacheDirectory = File(context.cacheDir, "video_media")
        val targetBytes = (context.cacheDir.totalSpace / 50L).coerceIn(
            MIN_CACHE_BYTES,
            MAX_CACHE_BYTES,
        )
        return SimpleCache(
            cacheDirectory,
            LeastRecentlyUsedCacheEvictor(targetBytes),
            StandaloneDatabaseProvider(context),
        )
    }

    private const val MIN_CACHE_BYTES = 64L * 1024L * 1024L
    private const val MAX_CACHE_BYTES = 256L * 1024L * 1024L
}
