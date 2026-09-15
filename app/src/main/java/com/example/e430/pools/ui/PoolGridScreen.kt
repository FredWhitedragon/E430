package com.example.e430.pools.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.e430.R
import com.example.e430.core.ui.RemotePreviewImage
import com.example.e430.pools.model.PoolPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun PoolGridRoute(
    request: PoolGridRequest,
    viewModel: PoolGridViewModel,
    modifier: Modifier = Modifier,
    onPoolClick: (PoolPreview) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(request) { viewModel.show(request) }
    PoolGridScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPoolClick = onPoolClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolGridScreen(
    uiState: PoolGridUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPoolClick: (PoolPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    val focusManager = LocalFocusManager.current
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect { focusManager.clearFocus() }
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
            uiState.isInitialLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) { CircularProgressIndicator() }
            uiState.hasError && uiState.items.isEmpty() -> ErrorState(onRetry)
            uiState.items.isEmpty() -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) { Text(stringResource(R.string.no_results)) }
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
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.refresh_failed),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
                items(uiState.items, key = PoolPreview::id) { pool ->
                    PoolCard(pool, onClick = { onPoolClick(pool) })
                }
                if (uiState.isLoadingMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) { CircularProgressIndicator() }
                    }
                }
                if (uiState.loadMoreFailed) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                        ) {
                            Text(stringResource(R.string.load_more_failed))
                            Button(onClick = onLoadMore) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PoolCard(pool: PoolPreview, onClick: () -> Unit) {
    var loadFailed by remember(pool.coverUrl) { mutableStateOf(false) }
    if (loadFailed || pool.coverUrl == null) return
    val aspectRatio = (pool.coverWidth.toFloat() / pool.coverHeight).coerceIn(0.7f, 1.6f)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        RemotePreviewImage(
            url = pool.coverUrl,
            contentDescription = stringResource(R.string.pool_preview),
            onLoadFailed = { loadFailed = true },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
        )
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = pool.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.pool_post_count,
                    pool.postCount,
                    pool.postCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
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
