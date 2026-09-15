package com.example.e430.pools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.e430.R
import com.example.e430.core.network.E430_USER_AGENT
import com.example.e430.core.network.E621Site
import com.example.e430.posts.detail.model.MediaKind
import com.example.e430.posts.detail.model.MediaQuality
import com.example.e430.posts.detail.model.PostDetail
import com.example.e430.settings.model.ImageQualityPreference
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import okhttp3.Headers.Companion.headersOf

@Composable
fun PoolDetailRoute(
    site: E621Site,
    poolId: Long,
    imageQuality: ImageQualityPreference,
    viewModel: PoolDetailViewModel,
    onPostClick: (Long, List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(site, poolId) { viewModel.show(site, poolId) }
    PoolDetailScreen(
        state = state,
        imageQuality = imageQuality,
        onVisiblePost = viewModel::loadWindow,
        onPostClick = { id -> state.pool?.postIds?.let { onPostClick(id, it) } },
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun PoolDetailScreen(
    state: PoolDetailUiState,
    imageQuality: ImageQualityPreference,
    onVisiblePost: (Int) -> Unit,
    onPostClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.loadFailed || state.pool == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.load_failed))
                Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
        else -> {
            val pool = state.pool
            val descriptionOffset = if (pool.description.isNotBlank()) 1 else 0
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = if (pool.postIds.isEmpty()) {
                    0
                } else {
                    state.visiblePostIndex + descriptionOffset
                },
            )
            LaunchedEffect(pool.id) { onVisiblePost(state.visiblePostIndex) }
            LaunchedEffect(listState, pool.postIds) {
                snapshotFlow {
                    listState.layoutInfo.visibleItemsInfo
                        .mapNotNull { it.key as? Long }
                        .minOfOrNull { pool.postIds.indexOf(it) }
                }
                    .mapNotNull { it }
                    .distinctUntilChanged()
                    .collect(onVisiblePost)
            }
            LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
                if (pool.description.isNotBlank()) {
                    item(key = "description") {
                        Text(
                            text = pool.description,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                if (pool.postIds.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        ) { Text(stringResource(R.string.no_results)) }
                    }
                }
                itemsIndexed(pool.postIds, key = { _, id -> id }) { _, id ->
                    PoolPostMedia(
                        post = state.posts[id],
                        failed = id in state.failedPostIds,
                        imageQuality = imageQuality,
                        onRetry = { onVisiblePost(pool.postIds.indexOf(id)) },
                        onPostClick = { onPostClick(id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PoolPostMedia(
    post: PostDetail?,
    failed: Boolean,
    imageQuality: ImageQualityPreference,
    onRetry: () -> Unit,
    onPostClick: () -> Unit,
) {
    if (post == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (failed) Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            else CircularProgressIndicator()
        }
        return
    }
    val preferredQuality = when (imageQuality) {
        ImageQualityPreference.Low -> MediaQuality.Low
        ImageQualityPreference.Medium -> MediaQuality.Medium
        ImageQualityPreference.Original -> MediaQuality.Original
    }
    val source = if (post.kind == MediaKind.Video) {
        post.mediaSources.firstOrNull { it.quality == MediaQuality.Low }
    } else {
        post.mediaSources.firstOrNull { it.quality == preferredQuality }
            ?: post.mediaSources.lastOrNull()
    }
    val model = source?.let {
        ImageRequest.Builder(LocalContext.current)
            .data(it.url)
            .headers(headersOf("User-Agent", E430_USER_AGENT))
            .build()
    }
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.post_preview),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio((post.width.toFloat() / post.height.coerceAtLeast(1)).coerceIn(0.25f, 3f))
                .let { if (post.kind == MediaKind.Video) it else it.clickable(onClick = onPostClick) },
        )
    }
    if (post.kind == MediaKind.Video) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(onClick = onPostClick, modifier = Modifier.padding(vertical = 10.dp)) {
                Text(stringResource(R.string.open_post_details))
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}
