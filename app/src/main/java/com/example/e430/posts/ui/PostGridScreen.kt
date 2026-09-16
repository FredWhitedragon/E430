package com.example.e430.posts.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import com.example.e430.R
import com.example.e430.core.network.E430_USER_AGENT
import com.example.e430.core.ui.NewPreviewEntrance
import com.example.e430.core.ui.RemotePreviewImage
import com.example.e430.posts.model.MediaPreview
import com.example.e430.posts.model.Rating
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.Headers.Companion.headersOf

@Composable
fun PostGridRoute(
    request: PostGridRequest,
    viewModel: PostGridViewModel,
    modifier: Modifier = Modifier,
    onPostClick: (Long) -> Unit = {},
    focusPostId: Long? = null,
    onFocusConsumed: () -> Unit = {},
    scrollToTopKey: Int = 0,
    prefetchEnabled: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(request) { viewModel.show(request) }
    PostGridScreen(
        animationKey = request,
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRemoveUnavailablePreviews = viewModel::removeUnavailablePreviews,
        onPostClick = onPostClick,
        focusPostId = focusPostId,
        onFocusConsumed = onFocusConsumed,
        scrollToTopKey = scrollToTopKey,
        prefetchEnabled = prefetchEnabled,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostGridScreen(
    animationKey: Any,
    uiState: PostGridUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onRemoveUnavailablePreviews: (Set<Long>) -> Unit,
    onPostClick: (Long) -> Unit,
    focusPostId: Long?,
    onFocusConsumed: () -> Unit,
    scrollToTopKey: Int,
    prefetchEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current
    var handledScrollToTopKey by remember { mutableIntStateOf(scrollToTopKey) }
    var pendingUnavailableIds by remember { mutableStateOf(emptySet<Long>()) }
    val animatedItemIds = remember(animationKey) { mutableSetOf<Any>() }
    LaunchedEffect(uiState.items) {
        animatedItemIds.retainAll(uiState.items.mapTo(mutableSetOf<Any>(), MediaPreview::id))
    }
    LaunchedEffect(scrollToTopKey) {
        if (scrollToTopKey != handledScrollToTopKey) {
            gridState.scrollToItem(0)
            handledScrollToTopKey = scrollToTopKey
        }
    }
    LaunchedEffect(focusPostId, uiState.items) {
        val index = uiState.items.indexOfFirst { it.id == focusPostId }
        if (index >= 0) {
            gridState.scrollToItem(index)
            androidx.compose.runtime.withFrameNanos { }
            val itemInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == focusPostId }
            if (itemInfo != null) {
                val viewportCenter = (
                    gridState.layoutInfo.viewportStartOffset + gridState.layoutInfo.viewportEndOffset
                ) / 2f
                val itemCenter = itemInfo.offset.y + itemInfo.size.height / 2f
                gridState.scrollBy(itemCenter - viewportCenter)
            }
            onFocusConsumed()
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    focusManager.clearFocus()
                } else if (pendingUnavailableIds.isNotEmpty()) {
                    val unavailableIds = pendingUnavailableIds
                    pendingUnavailableIds = emptySet()
                    onRemoveUnavailablePreviews(unavailableIds)
                }
            }
    }
    LaunchedEffect(gridState, uiState.items.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (uiState.items.isNotEmpty() && lastVisibleIndex >= uiState.items.lastIndex - 6) {
                    onLoadMore()
                }
            }
    }
    LaunchedEffect(gridState, uiState.items, prefetchEnabled) {
        if (!prefetchEnabled) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collectLatest { lastVisibleIndex ->
                val urls = uiState.items
                    .drop(lastVisibleIndex + 1)
                    .take(PREVIEW_PREFETCH_COUNT)
                    .mapNotNull(MediaPreview::previewUrl)
                coroutineScope {
                    urls.map { url ->
                        async {
                            context.imageLoader.execute(
                                ImageRequest.Builder(context)
                                    .data(url)
                                    .size(400)
                                    .headers(headersOf("User-Agent", E430_USER_AGENT))
                                    .build(),
                            )
                        }
                    }.awaitAll()
                }
            }
    }
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            uiState.isInitialLoading -> LoadingState()
            uiState.hasError && uiState.items.isEmpty() -> ErrorState(onRetry)
            uiState.items.isEmpty() -> EmptyState()
            else -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalItemSpacing = 0.dp,
                state = gridState,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uiState.hasError) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        RefreshError(onRetry = onRetry)
                    }
                }
                items(uiState.items, key = MediaPreview::id) { item ->
                    NewPreviewEntrance(
                        resultKey = animationKey,
                        itemKey = item.id,
                        animatedKeys = animatedItemIds,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MediaPreviewCard(
                            item = item,
                            onClick = { onPostClick(item.id) },
                            onPreviewUnavailable = { id ->
                                if (gridState.isScrollInProgress) {
                                    pendingUnavailableIds = pendingUnavailableIds + id
                                } else {
                                    onRemoveUnavailablePreviews(setOf(id))
                                }
                            },
                        )
                    }
                }
                if (uiState.isLoadingMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) { CircularProgressIndicator(modifier = Modifier.size(26.dp)) }
                    }
                }
                if (uiState.loadMoreFailed) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LoadMoreError(onRetry = onLoadMore)
                    }
                }
            }
        }
    }
}

@Composable
fun MediaPreviewCard(
    item: MediaPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPreviewUnavailable: (Long) -> Unit = {},
) {
    var loadFailed by remember(item.previewUrl) { mutableStateOf(false) }
    if (item.previewUrl == null) return
    val aspectRatio = (item.width.toFloat() / item.height).coerceIn(0.7f, 1.6f)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .graphicsLayer { alpha = if (loadFailed) 0f else 1f }
            .then(if (loadFailed) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        Box {
            RemotePreviewImage(
                url = item.previewUrl,
                contentDescription = stringResource(R.string.post_preview),
                onLoadFailed = {
                    if (!loadFailed) {
                        loadFailed = true
                        onPreviewUnavailable(item.id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
            )
            if (item.fileExtension.isNotBlank() && item.fileExtension.lowercase() !in STATIC_IMAGE_FORMATS) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = item.fileExtension.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Stat(
                value = item.score,
                icon = R.drawable.ic_score,
                description = R.string.score,
                modifier = Modifier.weight(1f),
            )
            Stat(
                value = item.favoriteCount,
                icon = R.drawable.ic_favorite,
                description = R.string.favorite_count,
                modifier = Modifier.weight(1f),
            )
            Stat(
                value = item.commentCount,
                icon = R.drawable.ic_comment,
                description = R.string.comment_count,
                modifier = Modifier.weight(1f),
            )
            RatingBadge(item.rating)
        }
    }
}

private val STATIC_IMAGE_FORMATS = setOf("jpg", "jpeg", "png", "webp", "avif")
private const val PREVIEW_PREFETCH_COUNT = 10

@Composable
private fun Stat(
    value: Int,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = value.toString(),
            fontSize = 11.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RatingBadge(rating: Rating) {
    val background = when (rating) {
        Rating.Safe -> Color(0xFF2E7D32)
        Rating.Questionable -> Color(0xFFF9A825)
        Rating.Explicit -> Color(0xFFC62828)
    }
    val description = when (rating) {
        Rating.Safe -> R.string.rating_safe
        Rating.Questionable -> R.string.rating_questionable
        Rating.Explicit -> R.string.rating_explicit
    }
    val ratingDescription = stringResource(description)
    Surface(
        color = background,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.semantics { contentDescription = ratingDescription },
    ) {
        Text(
            text = rating.code,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun LoadingState() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(stringResource(R.string.load_failed))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EmptyState() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.no_results))
    }
}

@Composable
private fun RefreshError(onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.refresh_failed),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun LoadMoreError(onRetry: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(
            text = stringResource(R.string.load_more_failed),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}
