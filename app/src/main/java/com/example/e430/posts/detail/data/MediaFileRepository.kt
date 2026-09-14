package com.example.e430.posts.detail.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.e430.posts.detail.model.MediaSource
import com.example.e430.posts.detail.model.PostDetail
import com.example.e430.core.network.E430_USER_AGENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class MediaFileRepository(private val client: OkHttpClient) {
    fun enqueueDownload(context: Context, post: PostDetail, source: MediaSource, directory: String): Boolean =
        runCatching {
            val folder = sanitizeFolder(directory)
            val extension = source.url.substringBefore('?').substringAfterLast('.', post.extension)
                .takeIf { it.length in 2..5 } ?: post.extension
            val filename = "${post.id}_${source.quality.name.lowercase()}.$extension"
            val request = DownloadManager.Request(source.url.toUri())
                .setTitle(filename)
                .addRequestHeader("User-Agent", E430_USER_AGENT)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$folder/$filename")
                .setAllowedOverMetered(true)
            context.getSystemService(DownloadManager::class.java).enqueue(request)
            true
        }.getOrDefault(false)

    suspend fun prepareShare(context: Context, post: PostDetail, source: MediaSource): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = client.newCall(Request.Builder().url(source.url).build()).execute()
                response.use {
                    if (!it.isSuccessful) return@runCatching null
                    val body = it.body ?: return@runCatching null
                    val directory = File(context.cacheDir, "shared_media").apply {
                        mkdirs()
                        listFiles()?.forEach(File::delete)
                    }
                    val extension = source.url.substringBefore('?').substringAfterLast('.', post.extension)
                        .takeIf { it.length in 2..5 } ?: post.extension
                    val file = File(directory, "${post.id}.$extension")
                    body.byteStream().use { input -> file.outputStream().use(input::copyTo) }
                    FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                }
            }.getOrNull()
        }

    fun sanitizeFolder(value: String): String = value
        .trim()
        .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
        .trim('.', ' ')
        .ifEmpty { "E430" }
}
