package com.example.e430.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.e430.core.network.E430_USER_AGENT
import okhttp3.Headers.Companion.headersOf

@Composable
fun RemotePreviewImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onLoadFailed: () -> Unit = {},
) {
    if (url == null) return

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .headers(headersOf("User-Agent", E430_USER_AGENT))
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        },
        error = { LaunchedEffect(url) { onLoadFailed() } },
        success = { SubcomposeAsyncImageContent() },
        modifier = modifier,
    )
}
