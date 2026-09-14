package com.example.e430.posts.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.e430.R
import com.example.e430.core.ui.RemotePreviewImage
import com.example.e430.posts.model.MediaPreview
import com.example.e430.posts.model.Rating
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PostGridRoute(
    request: PostGridRequest,
    viewModel: PostGridViewModel,
    modifier: Modifier = Modifier,
    onPostClick: (Long) -> Unit = {},
    focusPostId: Long? = null,
    onFocusConsumed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(request) { viewModel.show(request) }
    PostGridScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPostClick = onPostClick,
        focusPostId = focusPostId,
        onFocusConsumed = onFocusConsumed,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostGridScreen(
    uiState: PostGridUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (Long) -> Unit,
    focusPostId: Long?,
    onFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    LaunchedEffect(focusPostId, uiState.items) {
        val index = uiState.items.indexOfFirst { it.id == focusPostId }
        if (index >= 0) {
            gridState.scrollToItem(index)
            onFocusConsumed()
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
                    MediaPreviewCard(item, onClick = { onPostClick(item.id) })
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
private fun MediaPreviewCard(item: MediaPreview, onClick: () -> Unit) {
    var loadFailed by remember(item.previewUrl) { mutableStateOf(false) }
    if (loadFailed || item.previewUrl == null) return
    val aspectRatio = (item.width.toFloat() / item.height).coerceIn(0.7f, 1.6f)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        RemotePreviewImage(
            url = item.previewUrl,
            contentDescription = stringResource(R.string.post_preview),
            onLoadFailed = { loadFailed = true },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
        )
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
