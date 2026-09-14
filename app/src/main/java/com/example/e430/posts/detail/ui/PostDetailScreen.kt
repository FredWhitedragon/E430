package com.example.e430.posts.detail.ui

import android.net.ConnectivityManager
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.e430.R
import com.example.e430.account.model.Account
import com.example.e430.core.network.E430_USER_AGENT
import com.example.e430.core.network.E621Site
import com.example.e430.posts.detail.model.MediaKind
import com.example.e430.posts.detail.model.MediaQuality
import com.example.e430.posts.detail.model.MediaSource
import com.example.e430.posts.detail.model.PostComment
import com.example.e430.posts.detail.model.PostDetail
import com.example.e430.posts.model.Rating
import com.example.e430.settings.model.ImageQualityPreference
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import okhttp3.Headers.Companion.headersOf
import java.net.URI

@Composable
fun PostDetailRoute(
    site: E621Site,
    postId: Long,
    account: Account?,
    imageQualityPreference: ImageQualityPreference,
    videoAutoPlay: Boolean,
    videoMuted: Boolean,
    tagsCollapsedByDefault: Boolean,
    downloadDirectory: String,
    viewModel: PostDetailViewModel,
    onTagClick: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRequireLogin: () -> Unit,
    onDownload: (PostDetail, MediaSource) -> Unit,
    onShare: (PostDetail, MediaSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(site, postId) { viewModel.show(site, postId) }
    PostDetailScreen(
        state = state,
        account = account,
        imageQualityPreference = imageQualityPreference,
        videoAutoPlay = videoAutoPlay,
        videoMuted = videoMuted,
        tagsCollapsedByDefault = tagsCollapsedByDefault,
        downloadDirectory = downloadDirectory,
        onVote = { if (account == null) onRequireLogin() else viewModel.vote(it) },
        onFavorite = { if (account == null) onRequireLogin() else viewModel.toggleFavorite() },
        onCreateComment = { if (account == null) onRequireLogin() else viewModel.createComment(it) },
        onUpdateComment = viewModel::updateComment,
        onHideComment = viewModel::hideComment,
        onTagClick = onTagClick,
        onPrevious = onPrevious,
        onNext = onNext,
        onRequireLogin = onRequireLogin,
        onDownload = onDownload,
        onShare = onShare,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun PostDetailScreen(
    state: PostDetailUiState,
    account: Account?,
    imageQualityPreference: ImageQualityPreference,
    videoAutoPlay: Boolean,
    videoMuted: Boolean,
    tagsCollapsedByDefault: Boolean,
    downloadDirectory: String,
    onVote: (Int) -> Unit,
    onFavorite: () -> Unit,
    onCreateComment: (String) -> Unit,
    onUpdateComment: (Long, String) -> Unit,
    onHideComment: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRequireLogin: () -> Unit,
    onDownload: (PostDetail, MediaSource) -> Unit,
    onShare: (PostDetail, MediaSource) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    when {
        state.isLoading -> DetailLoadingSkeleton(modifier)
        state.loadFailed || state.post == null -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.load_failed))
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
        else -> {
            val post = state.post
            val context = LocalContext.current
            val automaticQuality = if (context.getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered) {
                MediaQuality.Low
            } else {
                MediaQuality.Medium
            }
            val preferredQuality = when (imageQualityPreference) {
                ImageQualityPreference.Auto -> automaticQuality
                ImageQualityPreference.Low -> MediaQuality.Low
                ImageQualityPreference.Medium -> MediaQuality.Medium
                ImageQualityPreference.Original -> MediaQuality.Original
            }
            var selectedQuality by rememberSaveable(post.id, preferredQuality) { mutableStateOf(preferredQuality) }
            val displayedSource = post.mediaSources.find { it.quality == selectedQuality }
                ?: post.mediaSources.lastOrNull()
            var horizontalDrag by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(post.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onHorizontalDrag = { _, amount -> horizontalDrag += amount },
                            onDragEnd = {
                                if (horizontalDrag > 160f) onPrevious()
                                if (horizontalDrag < -160f) onNext()
                            },
                        )
                    }
                    .verticalScroll(rememberScrollState()),
            ) {
                MediaSection(post, displayedSource, videoAutoPlay, videoMuted)
                MediaControlRow(
                    post = post,
                    displayedSource = displayedSource,
                    downloadDirectory = downloadDirectory,
                    onQualitySelected = { selectedQuality = it },
                    onDownload = onDownload,
                    onShare = onShare,
                )
                InteractionSection(post, onVote, onFavorite)
                if (state.actionFailed) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.action_failed),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                BasicInfo(post)
                TagsSection(post, tagsCollapsedByDefault, onTagClick)
                CommentsSection(
                    comments = state.comments,
                    account = account,
                    isActing = state.isActing,
                    onRequireLogin = onRequireLogin,
                    onCreate = onCreateComment,
                    onUpdate = onUpdateComment,
                    onHide = onHideComment,
                )
            }
        }
    }
}

@Composable
private fun DetailLoadingSkeleton(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            CircularProgressIndicator()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            LoadingBlock(width = 62.dp, height = 32.dp)
            LoadingBlock(width = 62.dp, height = 32.dp, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.weight(1f))
            LoadingBlock(width = 40.dp, height = 40.dp)
            LoadingBlock(width = 40.dp, height = 40.dp, modifier = Modifier.padding(start = 4.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            repeat(4) { LoadingBlock(width = 62.dp, height = 32.dp) }
        }
        SectionTitle(R.string.basic_information)
        repeat(5) { index ->
            LoadingBlock(
                width = if (index % 2 == 0) 260.dp else 210.dp,
                height = 16.dp,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            )
        }
        SectionTitle(R.string.tags)
        LoadingBlock(width = 92.dp, height = 32.dp, modifier = Modifier.padding(16.dp))
        SectionTitle(R.string.comments)
        LoadingBlock(
            width = 320.dp,
            height = 88.dp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun LoadingBlock(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun MediaSection(post: PostDetail, source: MediaSource?, autoPlay: Boolean, muted: Boolean) {
    if (source == null) {
        Text(
            stringResource(R.string.media_unavailable),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    if (post.kind == MediaKind.Video) {
        VideoPlayer(source.url, autoPlay, muted, post.width, post.height)
    } else {
        ZoomableImage(source.url, post.width, post.height)
    }
}

@Composable
private fun ZoomableImage(url: String, width: Int, height: Int) {
    var fullScreen by remember { mutableStateOf(false) }
    var loadFailed by remember(url) { mutableStateOf(false) }
    if (loadFailed) return
    val model = ImageRequest.Builder(LocalContext.current).data(url).headers(headersOf("User-Agent", E430_USER_AGENT)).build()
    AsyncImage(
        model = model,
        contentDescription = stringResource(R.string.post_preview),
        contentScale = ContentScale.Fit,
        onError = { loadFailed = true },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio((width.toFloat() / height.coerceAtLeast(1)).coerceIn(0.2f, 4f))
            .clickable { fullScreen = true },
    )
    if (fullScreen) FullScreenImage(model) { fullScreen = false }
}

@Composable
private fun FullScreenImage(model: ImageRequest, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var x by remember { mutableFloatStateOf(0f) }
        var y by remember { mutableFloatStateOf(0f) }
        val transform = rememberTransformableState { zoom, pan, _ -> scale = (scale * zoom).coerceIn(1f, 6f); x += pan.x; y += pan.y }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.full_screen_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = x, translationY = y)
                    .transformable(transform),
            )
        }
    }
}

@Composable
private fun VideoPlayer(url: String, autoPlay: Boolean, muted: Boolean, width: Int, height: Int) {
    val context = LocalContext.current
    var fullScreen by rememberSaveable(url) { mutableStateOf(false) }
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(DefaultHttpDataSource.Factory().setUserAgent(E430_USER_AGENT)))
            .build()
            .apply { setMediaItem(MediaItem.fromUri(url)); prepare() }
    }
    LaunchedEffect(autoPlay, muted) { player.playWhenReady = autoPlay; player.volume = if (muted) 0f else 1f }
    DisposableEffect(player) { onDispose { player.release() } }
    if (fullScreen) {
        FullScreenVideo(player = player, onDismiss = { fullScreen = false })
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio((width.toFloat() / height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f))
                .background(Color.Black),
        )
    } else {
        VideoPlayerSurface(
            player = player,
            onFullScreen = { fullScreen = true },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio((width.toFloat() / height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f)),
        )
    }
}

@Composable
private fun VideoPlayerSurface(
    player: ExoPlayer,
    onFullScreen: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = true } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onFullScreen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(bottomStart = 12.dp)),
        ) {
            Icon(
                painterResource(R.drawable.ic_fullscreen),
                stringResource(R.string.enter_fullscreen),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun FullScreenVideo(player: ExoPlayer, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { PlayerView(it).apply { this.player = player; useController = true } },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
            ) {
                Icon(
                    painterResource(R.drawable.ic_fullscreen_exit),
                    stringResource(R.string.exit_fullscreen),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun MediaControlRow(
    post: PostDetail,
    displayedSource: MediaSource?,
    downloadDirectory: String,
    onQualitySelected: (MediaQuality) -> Unit,
    onDownload: (PostDetail, MediaSource) -> Unit,
    onShare: (PostDetail, MediaSource) -> Unit,
) {
    var downloadDialog by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        if (post.kind != MediaKind.Video) {
            post.mediaSources.forEach { source ->
                TextButton(onClick = { onQualitySelected(source.quality) }) {
                    Text(
                        qualityName(source.quality),
                        color = if (source.quality == displayedSource?.quality) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ActionIcon(R.drawable.ic_download, R.string.download, MaterialTheme.colorScheme.onSurfaceVariant) {
            if (post.mediaSources.isNotEmpty()) downloadDialog = true
        }
        ActionIcon(R.drawable.ic_share, R.string.share, MaterialTheme.colorScheme.onSurfaceVariant) {
            displayedSource?.let { onShare(post, it) }
        }
    }
    if (downloadDialog) {
        DownloadDialog(post, downloadDirectory, { downloadDialog = false }) {
            onDownload(post, it)
            downloadDialog = false
        }
    }
}

@Composable
private fun InteractionSection(post: PostDetail, onVote: (Int) -> Unit, onFavorite: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        ActionIcon(R.drawable.ic_arrow_up, R.string.upvote, if (post.userVote > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant) { onVote(1) }
        Text(post.score.toString(), color = when { post.userVote > 0 -> Color(0xFF2E7D32); post.userVote < 0 -> Color(0xFFC62828); else -> MaterialTheme.colorScheme.onSurface })
        ActionIcon(R.drawable.ic_arrow_down, R.string.downvote, if (post.userVote < 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant) { onVote(-1) }
        ActionIcon(R.drawable.ic_favorite, if (post.isFavorited) R.string.remove_favorite else R.string.add_favorite, if (post.isFavorited) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant, onFavorite)
        Text(post.favoriteCount.toString(), color = if (post.isFavorited) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface)
        RatingText(post.rating)
    }
}

@Composable private fun ActionIcon(icon: Int, description: Int, tint: Color, onClick: () -> Unit) = IconButton(onClick = onClick) { Icon(painterResource(icon), stringResource(description), tint = tint) }

@Composable private fun RatingText(rating: Rating) {
    val color = when (rating) { Rating.Safe -> Color(0xFF2E7D32); Rating.Questionable -> Color(0xFFF9A825); Rating.Explicit -> Color(0xFFC62828) }
    val text = when (rating) { Rating.Safe -> R.string.rating_safe; Rating.Questionable -> R.string.rating_questionable; Rating.Explicit -> R.string.rating_explicit }
    Text(stringResource(text), color = color, modifier = Modifier.padding(start = 8.dp))
}

@Composable private fun BasicInfo(post: PostDetail) {
    SectionTitle(R.string.basic_information)
    InfoRow(R.string.format, post.extension.uppercase())
    InfoRow(R.string.dimensions, stringResource(R.string.dimensions_value, post.width, post.height))
    InfoRow(R.string.file_size, formatBytes(post.fileSize))
    if (post.md5.isNotBlank()) InfoRow(R.string.md5, post.md5)
    post.durationSeconds?.let { InfoRow(R.string.duration, stringResource(R.string.seconds_value, it)) }
    InfoRow(R.string.score_breakdown, stringResource(R.string.score_breakdown_value, post.upScore, post.downScore))
    InfoRow(R.string.comment_count, post.commentCount.toString())
    InfoRow(R.string.uploader, stringResource(R.string.user_with_id, post.uploaderName, post.uploaderId))
    InfoRow(R.string.uploaded_at, post.createdAt)
    if (post.sources.isNotEmpty()) SourceLinks(post.sources)
    if (post.pools.isNotEmpty()) InfoRow(R.string.pool_ids, post.pools.joinToString())
    post.parentId?.let { InfoRow(R.string.parent_post, it.toString()) }
    if (post.childIds.isNotEmpty()) InfoRow(R.string.child_posts, post.childIds.joinToString())
    InfoRow(R.string.has_notes, stringResource(if (post.hasNotes) R.string.yes else R.string.no))
    if (post.statusFlags.isNotEmpty()) InfoRow(R.string.status_flags, statusFlagsText(post.statusFlags))
    if (post.description.isNotBlank()) InfoRow(R.string.description, post.description)
}

@Composable
private fun SourceLinks(sources: List<String>) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            stringResource(R.string.source_links),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(.35f),
        )
        Column(Modifier.weight(.65f)) {
            sources.forEach { source ->
                val canOpen = remember(source) { isHttpUrl(source) }
                Text(
                    text = source,
                    color = if (canOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (canOpen) TextDecoration.Underline else TextDecoration.None,
                    modifier = if (canOpen) {
                        Modifier.clickable {
                            runCatching { uriHandler.openUri(source) }
                                .onFailure {
                                    Toast.makeText(context, R.string.open_link_failed, Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

private fun isHttpUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
}.getOrDefault(false)

@Composable
private fun InfoRow(label: Int, value: String) = Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
    Text(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.35f))
    Text(value, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(.65f))
}

@Composable
private fun SectionTitle(label: Int) {
    HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable private fun TagsSection(post: PostDetail, collapsedDefault: Boolean, onTagClick: (String) -> Unit) {
    var collapsed by rememberSaveable(post.id) { mutableStateOf(collapsedDefault) }
    SectionTitle(R.string.tags)
    TextButton(onClick = { collapsed = !collapsed }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(if (collapsed) R.string.expand else R.string.collapse)) }
    if (!collapsed) post.tags.forEach { (category, tags) ->
        Text(
            tagCategoryName(category),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        TagWrapLayout(modifier = Modifier.padding(horizontal = 12.dp)) {
            tags.forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.clickable { onTagClick(tag) },
                ) {
                    Text(
                        tag,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

/** A small wrapping layout that avoids the version-sensitive Foundation FlowRow API. */
@Composable
private fun TagWrapLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val horizontalGap = 6.dp
    val verticalGap = 6.dp
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val horizontalGapPx = horizontalGap.roundToPx()
        val verticalGapPx = verticalGap.roundToPx()
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)
        var x = 0
        var y = 0
        var rowHeight = 0
        placeables.forEach { placeable ->
            if (x > 0 && x + placeable.width > constraints.maxWidth) {
                x = 0
                y += rowHeight + verticalGapPx
                rowHeight = 0
            }
            positions += x to y
            x += placeable.width + horizontalGapPx
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        val height = (if (placeables.isEmpty()) 0 else y + rowHeight)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val (positionX, positionY) = positions[index]
                placeable.placeRelative(positionX, positionY)
            }
        }
    }
}

@Composable private fun CommentsSection(comments: List<PostComment>, account: Account?, isActing: Boolean, onRequireLogin: () -> Unit, onCreate: (String) -> Unit, onUpdate: (Long, String) -> Unit, onHide: (Long) -> Unit) {
    var body by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<PostComment?>(null) }
    SectionTitle(R.string.comments)
    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(stringResource(R.string.comment_hint)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    Button(onClick = { if (account == null) onRequireLogin() else { onCreate(body); body = "" } }, enabled = !isActing && body.isNotBlank(), modifier = Modifier.padding(16.dp)) { Text(stringResource(R.string.post_comment)) }
    if (comments.isEmpty()) {
        Text(
            stringResource(R.string.no_comments),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
    comments.forEach { comment ->
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                stringResource(R.string.comment_by, comment.creatorName, comment.createdAt.take(10)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(comment.body, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 6.dp))
            if (account?.id == comment.creatorId) Row {
                TextButton(onClick = { editing = comment }) { Text(stringResource(R.string.edit_comment)) }
                if (!comment.isHidden) TextButton(onClick = { onHide(comment.id) }) { Text(stringResource(R.string.hide_comment)) }
            }
        }
    }
    editing?.let { comment -> EditCommentDialog(comment, { editing = null }) { onUpdate(comment.id, it); editing = null } }
}

@Composable private fun EditCommentDialog(comment: PostComment, onDismiss: () -> Unit, onSave: (String) -> Unit) { var value by remember(comment.id) { mutableStateOf(comment.body) }; AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.edit_comment)) }, text = { OutlinedTextField(value, { value = it }) }, confirmButton = { TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) }

@Composable private fun DownloadDialog(post: PostDetail, directory: String, onDismiss: () -> Unit, onConfirm: (MediaSource) -> Unit) { var selected by remember { mutableStateOf(post.mediaSources.last()) }; AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.download_media)) }, text = { Column { Text(stringResource(R.string.download_location, directory.ifBlank { "E430" }), modifier = Modifier.padding(bottom = 8.dp)); post.mediaSources.forEach { source -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selected = source }) { RadioButton(selected == source, { selected = source }); Text(stringResource(R.string.estimated_size, qualityName(source.quality), formatBytes(source.estimatedBytes))) } } } }, confirmButton = { Button(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.start_download)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) }

@Composable private fun qualityName(quality: MediaQuality) = stringResource(when (quality) { MediaQuality.Low -> R.string.quality_low; MediaQuality.Medium -> R.string.quality_medium; MediaQuality.Original -> R.string.quality_original })
@Composable private fun formatBytes(bytes: Long): String = if (bytes <= 0) stringResource(R.string.size_unknown) else Formatter.formatFileSize(LocalContext.current, bytes)
@Composable private fun tagCategoryName(category: String) = stringResource(when (category) {
    "artist" -> R.string.tag_artist
    "copyright" -> R.string.tag_copyright
    "character" -> R.string.tag_character
    "species" -> R.string.tag_species
    "meta" -> R.string.tag_meta
    "lore" -> R.string.tag_lore
    "contributor" -> R.string.tag_contributor
    "invalid" -> R.string.tag_invalid
    else -> R.string.tag_general
})
@Composable private fun statusFlagName(flag: String) = stringResource(when (flag) {
    "pending" -> R.string.status_pending
    "flagged" -> R.string.status_flagged
    "note_locked" -> R.string.status_note_locked
    "status_locked" -> R.string.status_status_locked
    "rating_locked" -> R.string.status_rating_locked
    else -> R.string.status_deleted
})
@Composable private fun statusFlagsText(flags: List<String>): String {
    val labels = mutableListOf<String>()
    for (flag in flags) labels += statusFlagName(flag)
    return labels.joinToString()
}
