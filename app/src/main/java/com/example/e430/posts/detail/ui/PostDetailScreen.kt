package com.example.e430.posts.detail.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.provider.Settings
import android.os.SystemClock
import android.text.format.Formatter
import android.view.Window
import java.util.WeakHashMap
import androidx.compose.animation.AnimatedVisibility
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import com.example.e430.posts.detail.model.PoolNavigationInfo
import com.example.e430.posts.detail.data.VideoPlaybackCache
import com.example.e430.posts.model.Rating
import com.example.e430.posts.model.MediaPreview
import com.example.e430.posts.ui.MediaPreviewCard
import com.example.e430.settings.model.ImageQualityPreference
import com.example.e430.settings.model.VideoGestureSettings
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import okhttp3.Headers.Companion.headersOf
import java.net.URI
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PostDetailRoute(
    site: E621Site,
    postId: Long,
    account: Account?,
    meteredImageQualityPreference: ImageQualityPreference,
    wifiImageQualityPreference: ImageQualityPreference,
    videoAutoPlay: Boolean,
    videoMuted: Boolean,
    videoLoop: Boolean,
    videoGestureSettings: VideoGestureSettings,
    prefetchPostIds: List<Long>,
    prefetchEnabled: Boolean,
    tagsCollapsedByDefault: Boolean,
    downloadDirectory: String,
    viewModel: PostDetailViewModel,
    onTagClick: (String) -> Unit,
    poolLinks: List<PoolNavigationInfo>,
    onPoolClick: (PoolNavigationInfo) -> Unit,
    onPoolPostClick: (PoolNavigationInfo, Int, Int) -> Unit,
    onRelatedPostClick: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRequireLogin: () -> Unit,
    onDownload: (PostDetail, MediaSource) -> Unit,
    onShare: (PostDetail, MediaSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(site, postId) { viewModel.show(site, postId) }
    val context = LocalContext.current
    val isMetered = context.getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered
    val activePreference = if (isMetered) meteredImageQualityPreference else wifiImageQualityPreference
    val prefetchQuality = when (activePreference) {
        ImageQualityPreference.Low -> MediaQuality.Low
        ImageQualityPreference.Medium -> MediaQuality.Medium
        ImageQualityPreference.Original -> MediaQuality.Original
    }
    LaunchedEffect(state.post?.id, prefetchPostIds, prefetchEnabled, prefetchQuality) {
        if (state.post?.id == postId && prefetchEnabled) {
            viewModel.prefetch(site, prefetchPostIds, prefetchQuality, preloadImages = true)
        }
    }
    val displayedState = when {
        state.post?.id == postId -> state
        else -> viewModel.cachedState(site, postId) ?: PostDetailUiState()
    }
    PostDetailScreen(
        state = displayedState,
        account = account,
        imageQualityPreference = activePreference,
        videoAutoPlay = videoAutoPlay,
        videoMuted = videoMuted,
        videoLoop = videoLoop,
        videoGestureSettings = videoGestureSettings,
        tagsCollapsedByDefault = tagsCollapsedByDefault,
        downloadDirectory = downloadDirectory,
        onVote = { if (account == null) onRequireLogin() else viewModel.vote(it) },
        onFavorite = { if (account == null) onRequireLogin() else viewModel.toggleFavorite() },
        onCreateComment = { if (account == null) onRequireLogin() else viewModel.createComment(it) },
        onUpdateComment = viewModel::updateComment,
        onHideComment = viewModel::hideComment,
        onTagClick = onTagClick,
        poolLinks = poolLinks,
        onPoolClick = onPoolClick,
        onPoolPostClick = onPoolPostClick,
        onRelatedPostClick = onRelatedPostClick,
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
    videoLoop: Boolean,
    videoGestureSettings: VideoGestureSettings,
    tagsCollapsedByDefault: Boolean,
    downloadDirectory: String,
    onVote: (Int) -> Unit,
    onFavorite: () -> Unit,
    onCreateComment: (String) -> Unit,
    onUpdateComment: (Long, String) -> Unit,
    onHideComment: (Long) -> Unit,
    onTagClick: (String) -> Unit,
    poolLinks: List<PoolNavigationInfo>,
    onPoolClick: (PoolNavigationInfo) -> Unit,
    onPoolPostClick: (PoolNavigationInfo, Int, Int) -> Unit,
    onRelatedPostClick: (Long) -> Unit,
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
            val preferredQuality = when (imageQualityPreference) {
                ImageQualityPreference.Low -> MediaQuality.Low
                ImageQualityPreference.Medium -> MediaQuality.Medium
                ImageQualityPreference.Original -> MediaQuality.Original
            }
            var selectedQuality by rememberSaveable(post.id, preferredQuality) { mutableStateOf(preferredQuality) }
            val displayedSource = post.mediaSources.find { it.quality == selectedQuality }
                ?: post.mediaSources.minByOrNull {
                    kotlin.math.abs(it.quality.ordinal - selectedQuality.ordinal)
                }
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
                PoolNavigationSection(
                    postId = post.id,
                    pools = poolLinks,
                    onPoolClick = onPoolClick,
                    onPostClick = onPoolPostClick,
                )
                MediaSection(
                    post = post,
                    source = displayedSource,
                    autoPlay = videoAutoPlay,
                    muted = videoMuted,
                    loop = videoLoop,
                    videoGestureSettings = videoGestureSettings,
                    onQualitySelected = { selectedQuality = it },
                )
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
                BasicInfo(
                    post = post,
                    relatedPreviews = state.relatedPreviews,
                    relatedPreviewsLoaded = state.relatedPreviewsLoaded,
                    onRelatedPostClick = onRelatedPostClick,
                )
                TagsSection(post, tagsCollapsedByDefault, onTagClick)
                CommentsSection(
                    comments = state.comments,
                    isLoading = state.isCommentsLoading,
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
private fun MediaSection(
    post: PostDetail,
    source: MediaSource?,
    autoPlay: Boolean,
    muted: Boolean,
    loop: Boolean,
    videoGestureSettings: VideoGestureSettings,
    onQualitySelected: (MediaQuality) -> Unit,
) {
    if (source == null) {
        Text(
            stringResource(R.string.media_unavailable),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    if (post.kind == MediaKind.Video) {
        VideoPlayer(
            sources = post.mediaSources,
            selectedSource = source,
            autoPlay = autoPlay,
            muted = muted,
            loop = loop,
            gestureSettings = videoGestureSettings,
            width = post.width,
            height = post.height,
            onQualitySelected = onQualitySelected,
        )
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
@androidx.annotation.OptIn(UnstableApi::class)
private fun VideoPlayer(
    sources: List<MediaSource>,
    selectedSource: MediaSource,
    autoPlay: Boolean,
    muted: Boolean,
    loop: Boolean,
    gestureSettings: VideoGestureSettings,
    width: Int,
    height: Int,
    onQualitySelected: (MediaQuality) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activityWindow = context.findActivity()?.window
    var fullScreen by rememberSaveable(sources) { mutableStateOf(false) }
    var volume by rememberSaveable(sources) { mutableFloatStateOf(if (muted) 0f else 1f) }
    var volumeBeforeMute by rememberSaveable(sources) { mutableFloatStateOf(1f) }
    var isLooping by rememberSaveable(sources) { mutableStateOf(loop) }
    var playbackSpeed by rememberSaveable(sources) { mutableFloatStateOf(1f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(!autoPlay) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by rememberSaveable(sources) { mutableStateOf(true) }
    var controlMenuExpanded by remember { mutableStateOf(false) }
    var controlInteraction by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackRevision by remember { mutableIntStateOf(0) }
    var videoSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var lastDragFinishedAt by remember { mutableLongStateOf(0L) }
    val originalBrightness = remember(activityWindow) {
        activityWindow?.let(VideoBrightnessSessions::acquire)
    }
    var brightness by remember(activityWindow) {
        mutableFloatStateOf(currentWindowBrightness(context, originalBrightness))
    }
    var brightnessAdjusted by remember(activityWindow) { mutableStateOf(false) }
    val player = remember(sources) {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBackBuffer(VIDEO_BACK_BUFFER_MILLIS, true)
                    .build(),
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(
                    VideoPlaybackCache.dataSourceFactory(context),
                ),
            )
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(selectedSource.url))
                volume = if (muted) 0f else 1f
                repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                prepare()
                playWhenReady = autoPlay
            }
    }
    var loadedUrl by remember(player) { mutableStateOf(selectedSource.url) }
    val isMuted = volume <= 0.001f
    LaunchedEffect(selectedSource.url) {
        if (loadedUrl != selectedSource.url) {
            val resumePosition = player.currentPosition.coerceAtLeast(0L)
            val resumePlayback = player.playWhenReady
            player.setMediaItem(MediaItem.fromUri(selectedSource.url))
            player.prepare()
            player.seekTo(resumePosition)
            player.playWhenReady = resumePlayback
            loadedUrl = selectedSource.url
        }
    }
    LaunchedEffect(autoPlay) { player.playWhenReady = autoPlay }
    LaunchedEffect(muted) {
        if (muted) {
            if (volume > 0.001f) volumeBeforeMute = volume
            volume = 0f
        } else if (volume <= 0.001f) {
            volume = volumeBeforeMute.coerceAtLeast(0.01f)
        }
    }
    LaunchedEffect(volume) { player.volume = volume.coerceIn(0f, 1f) }
    LaunchedEffect(isLooping) {
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
    LaunchedEffect(playbackSpeed) { player.setPlaybackSpeed(playbackSpeed) }
    LaunchedEffect(isPlaying, controlsVisible, controlInteraction, controlMenuExpanded) {
        if (isPlaying && controlsVisible && !controlMenuExpanded) {
            delay(CONTROLS_HIDE_DELAY_MILLIS)
            controlsVisible = false
        }
    }
    LaunchedEffect(feedbackRevision) {
        if (feedbackRevision > 0) {
            delay(GESTURE_FEEDBACK_MILLIS)
            feedback = null
        }
    }
    LaunchedEffect(player) {
        while (currentCoroutineContext().isActive) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L } ?: 0L
            delay(400)
        }
    }
    val latestBrightnessAdjusted by rememberUpdatedState(brightnessAdjusted)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isPaused = !playWhenReady
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) isPaused = true
            }
        }
        player.addListener(listener)
        isPlaying = player.isPlaying
        isPaused = !player.playWhenReady || player.playbackState == Player.STATE_ENDED
        onDispose {
            player.removeListener(listener)
            player.release()
            if (latestBrightnessAdjusted && activityWindow != null && originalBrightness != null) {
                activityWindow.setScreenBrightness(originalBrightness)
            }
            activityWindow?.let(VideoBrightnessSessions::release)
        }
    }
    fun showFeedback(message: String) {
        feedback = message
        feedbackRevision += 1
    }
    fun revealControls() {
        controlsVisible = true
        controlInteraction += 1
    }
    fun togglePlayback(revealControlsAfter: Boolean = true) {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
        if (revealControlsAfter) revealControls()
    }
    fun seekBy(seconds: Int) {
        val target = (player.currentPosition + seconds * 1_000L)
            .coerceAtLeast(0L)
            .let { if (duration > 0L) it.coerceAtMost(duration) else it }
        player.seekTo(target)
        showFeedback(
            resources.getString(
                if (seconds < 0) R.string.seek_backward_feedback else R.string.seek_forward_feedback,
                kotlin.math.abs(seconds),
            ),
        )
        revealControls()
    }
    val gestureModifier = Modifier
        .fillMaxSize()
        .onSizeChanged { videoSize = it }
        .pointerInput(player, gestureSettings, fullScreen) {
            detectTapGestures(
                onTap = {
                    val now = SystemClock.uptimeMillis()
                    if (lastDragFinishedAt == 0L || now - lastDragFinishedAt > TAP_AFTER_DRAG_GUARD_MILLIS) {
                        controlsVisible = !controlsVisible
                        if (controlsVisible) controlInteraction += 1
                    }
                },
                onDoubleTap = { offset ->
                    val region = offset.x / size.width.coerceAtLeast(1)
                    when {
                        region < 1f / 3f && gestureSettings.doubleTapRewind ->
                            seekBy(-gestureSettings.rewindSeconds)
                        region > 2f / 3f && gestureSettings.doubleTapForward ->
                            seekBy(gestureSettings.forwardSeconds)
                        region in 1f / 3f..2f / 3f && gestureSettings.doubleTapPlayPause ->
                            togglePlayback()
                    }
                },
            )
        }
        .then(
            if (gestureSettings.horizontalSwipeSeek ||
                (fullScreen && (
                    gestureSettings.fullscreenBrightnessSwipe ||
                        gestureSettings.fullscreenVolumeSwipe
                    ))
            ) {
                Modifier.pointerInput(player, gestureSettings, fullScreen, videoSize) {
                    var totalX = 0f
                    var totalY = 0f
                    var dragMode = VideoDragMode.None
                    var startPosition = 0L
                    var startVolume = 0f
                    var startBrightness = 0f
                    var dragStartX = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            totalX = 0f
                            totalY = 0f
                            dragMode = VideoDragMode.None
                            startPosition = player.currentPosition.coerceAtLeast(0L)
                            startVolume = volume
                            startBrightness = brightness
                            dragStartX = offset.x
                        },
                        onDrag = { change, amount ->
                            totalX += amount.x
                            totalY += amount.y
                            if (dragMode == VideoDragMode.None) {
                                dragMode = if (kotlin.math.abs(totalX) >= kotlin.math.abs(totalY)) {
                                    if (gestureSettings.horizontalSwipeSeek) VideoDragMode.Seek
                                    else VideoDragMode.Ignored
                                } else if (fullScreen && dragStartX < size.width / 2f) {
                                    if (gestureSettings.fullscreenBrightnessSwipe) VideoDragMode.Brightness
                                    else VideoDragMode.Ignored
                                } else if (fullScreen) {
                                    if (gestureSettings.fullscreenVolumeSwipe) VideoDragMode.Volume
                                    else VideoDragMode.Ignored
                                } else {
                                    VideoDragMode.Ignored
                                }
                            }
                            if (dragMode != VideoDragMode.Ignored) change.consume()
                            when (dragMode) {
                                VideoDragMode.Seek -> {
                                    val fraction = (kotlin.math.abs(totalX) / size.width.coerceAtLeast(1))
                                        .coerceIn(0f, 1f)
                                    val seconds = (1f + fraction * 59f).toInt().coerceIn(1, 60)
                                    val signedSeconds = if (totalX < 0f) -seconds else seconds
                                    val target = (startPosition + signedSeconds * 1_000L)
                                        .coerceAtLeast(0L)
                                        .let { if (duration > 0L) it.coerceAtMost(duration) else it }
                                    player.seekTo(target)
                                    showFeedback(
                                        resources.getString(
                                            if (signedSeconds < 0) R.string.seek_backward_feedback
                                            else R.string.seek_forward_feedback,
                                            kotlin.math.abs(signedSeconds),
                                        ),
                                    )
                                }
                                VideoDragMode.Brightness -> {
                                    brightness = (startBrightness - totalY / size.height.coerceAtLeast(1))
                                        .coerceIn(0.01f, 1f)
                                    brightnessAdjusted = true
                                    activityWindow?.setScreenBrightness(brightness)
                                    showFeedback(
                                        resources.getString(
                                            R.string.brightness_feedback,
                                            (brightness * 100).toInt(),
                                        ),
                                    )
                                }
                                VideoDragMode.Volume -> {
                                    volume = (startVolume - totalY / size.height.coerceAtLeast(1))
                                        .coerceIn(0f, 1f)
                                    if (volume > 0.001f) volumeBeforeMute = volume
                                    showFeedback(
                                        resources.getString(
                                            R.string.volume_feedback,
                                            (volume * 100).toInt(),
                                        ),
                                    )
                                }
                                else -> Unit
                            }
                        },
                        onDragEnd = {
                            lastDragFinishedAt = SystemClock.uptimeMillis()
                            revealControls()
                        },
                        onDragCancel = {
                            lastDragFinishedAt = SystemClock.uptimeMillis()
                            revealControls()
                        },
                    )
                }
            } else {
                Modifier
            },
        )
    val controls: @Composable (Boolean, () -> Unit) -> Unit = { isFullScreen, onFullScreen ->
        VideoControls(
            isPlaying = !isPaused,
            isMuted = isMuted,
            isLooping = isLooping,
            playbackSpeed = playbackSpeed,
            position = position,
            duration = duration,
            isFullScreen = isFullScreen,
            sources = sources,
            selectedSource = selectedSource,
            onPlayPause = {
                togglePlayback()
            },
            onSeek = { player.seekTo(it); revealControls() },
            onMutedChange = { shouldMute ->
                if (shouldMute) {
                    if (volume > 0.001f) volumeBeforeMute = volume
                    volume = 0f
                } else {
                    volume = volumeBeforeMute.coerceAtLeast(0.01f)
                }
                revealControls()
            },
            onLoopingChange = { isLooping = it; revealControls() },
            onSpeedChange = { playbackSpeed = it; revealControls() },
            onQualitySelected = { onQualitySelected(it); revealControls() },
            onMenuExpandedChange = { expanded ->
                if (controlMenuExpanded != expanded) {
                    controlMenuExpanded = expanded
                    if (!expanded) revealControls()
                }
            },
            onFullScreen = { onFullScreen(); revealControls() },
        )
    }
    if (fullScreen) {
        FullScreenVideo(
            player = player,
            controlsVisible = controlsVisible,
            isPaused = isPaused,
            onPlay = { togglePlayback(revealControlsAfter = false) },
            controls = { controls(true) { fullScreen = false } },
            gestureModifier = gestureModifier,
            feedback = feedback,
            screenBrightness = brightness.takeIf { brightnessAdjusted },
            onDismiss = { fullScreen = false },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio((width.toFloat() / height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f))
                .background(Color.Black),
        )
    } else {
        VideoPlayerSurface(
            player = player,
            controlsVisible = controlsVisible,
            isPaused = isPaused,
            onPlay = { togglePlayback(revealControlsAfter = false) },
            controls = { controls(false) { fullScreen = true } },
            gestureModifier = gestureModifier,
            feedback = feedback,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio((width.toFloat() / height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f)),
        )
    }
}

@Composable
private fun VideoPlayerSurface(
    player: ExoPlayer,
    controlsVisible: Boolean,
    isPaused: Boolean,
    onPlay: () -> Unit,
    controls: @Composable () -> Unit,
    gestureModifier: Modifier,
    feedback: String?,
    modifier: Modifier,
) {
    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = false } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        Box(gestureModifier)
        if (isPaused) {
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onPlay, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }
        feedback?.let { VideoGestureFeedback(it) }
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { controls() }
    }
}

@Composable
private fun FullScreenVideo(
    player: ExoPlayer,
    controlsVisible: Boolean,
    isPaused: Boolean,
    onPlay: () -> Unit,
    controls: @Composable () -> Unit,
    gestureModifier: Modifier,
    feedback: String?,
    screenBrightness: Float?,
    onDismiss: () -> Unit,
) {
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
        LaunchedEffect(window, screenBrightness) {
            if (window != null && screenBrightness != null) {
                window.setScreenBrightness(screenBrightness)
            }
        }
        VideoPlayerSurface(
            player = player,
            controlsVisible = controlsVisible,
            isPaused = isPaused,
            onPlay = onPlay,
            controls = controls,
            gestureModifier = gestureModifier,
            feedback = feedback,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun VideoControls(
    isPlaying: Boolean,
    isMuted: Boolean,
    isLooping: Boolean,
    playbackSpeed: Float,
    position: Long,
    duration: Long,
    isFullScreen: Boolean,
    sources: List<MediaSource>,
    selectedSource: MediaSource,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onLoopingChange: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onQualitySelected: (MediaQuality) -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    onFullScreen: () -> Unit,
) {
    var speedMenuVisible by remember { mutableStateOf(false) }
    var qualityMenuVisible by remember { mutableStateOf(false) }
    LaunchedEffect(speedMenuVisible, qualityMenuVisible) {
        onMenuExpandedChange(speedMenuVisible || qualityMenuVisible)
    }
    DisposableEffect(Unit) {
        onDispose { onMenuExpandedChange(false) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.72f)),
    ) {
        Slider(
            value = position.coerceAtMost(duration.coerceAtLeast(1L)).toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(44.dp)) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    stringResource(if (isPlaying) R.string.pause else R.string.play),
                    tint = Color.White,
                )
            }
            Text(
                text = formatPlaybackTime(position, duration),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.width(6.dp))
            Box {
                TextButton(onClick = { qualityMenuVisible = true }) {
                    Text(qualityName(selectedSource.quality), color = Color.White)
                }
                DropdownMenu(
                    expanded = qualityMenuVisible,
                    onDismissRequest = { qualityMenuVisible = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.video_quality)) },
                        onClick = {},
                        enabled = false,
                    )
                    sources.forEach { source ->
                        DropdownMenuItem(
                            text = {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(qualityName(source.quality), modifier = Modifier.weight(1f))
                                    Text(
                                        stringResource(
                                            R.string.dimensions_value,
                                            source.width,
                                            source.height,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                qualityMenuVisible = false
                                onQualitySelected(source.quality)
                            },
                        )
                    }
                }
            }
            Box {
                TextButton(onClick = { speedMenuVisible = true }) {
                    Text(stringResource(R.string.playback_speed_value, playbackSpeed), color = Color.White)
                }
                DropdownMenu(
                    expanded = speedMenuVisible,
                    onDismissRequest = { speedMenuVisible = false },
                ) {
                    PLAYBACK_SPEEDS.forEach { speed ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.playback_speed_value, speed)) },
                            onClick = {
                                speedMenuVisible = false
                                onSpeedChange(speed)
                            },
                        )
                    }
                }
            }
            IconButton(onClick = { onLoopingChange(!isLooping) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    painterResource(R.drawable.ic_repeat),
                    stringResource(if (isLooping) R.string.disable_loop else R.string.enable_loop),
                    tint = if (isLooping) Color(0xFFFCBF31) else Color.White,
                )
            }
            IconButton(onClick = { onMutedChange(!isMuted) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    painterResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on),
                    stringResource(if (isMuted) R.string.unmute else R.string.mute),
                    tint = if (isMuted) Color(0xFFFCBF31) else Color.White,
                )
            }
            IconButton(onClick = onFullScreen, modifier = Modifier.size(44.dp)) {
                Icon(
                    painterResource(if (isFullScreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen),
                    stringResource(if (isFullScreen) R.string.exit_fullscreen else R.string.enter_fullscreen),
                    tint = Color.White,
                )
            }
        }
    }
}

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private const val CONTROLS_HIDE_DELAY_MILLIS = 3_000L
private const val GESTURE_FEEDBACK_MILLIS = 700L
private const val TAP_AFTER_DRAG_GUARD_MILLIS = 250L
private const val VIDEO_BACK_BUFFER_MILLIS = 120_000

internal fun formatPlaybackTime(positionMillis: Long, durationMillis: Long): String =
    "${formatMinutesSeconds(positionMillis)}/${formatMinutesSeconds(durationMillis)}"

private fun formatMinutesSeconds(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(java.util.Locale.ROOT, minutes, seconds)
}

private enum class VideoDragMode { None, Seek, Brightness, Volume, Ignored }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun currentWindowBrightness(context: Context, windowBrightness: Float?): Float {
    if (windowBrightness != null && windowBrightness >= 0f) return windowBrightness
    return runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.5f).coerceIn(0.01f, 1f)
}

private fun Window.setScreenBrightness(value: Float) {
    attributes = attributes.apply { screenBrightness = value }
}

/** Keeps the pre-video brightness stable while old and new detail pages overlap during animation. */
private object VideoBrightnessSessions {
    private data class Entry(val original: Float, var holders: Int)

    private val entries = WeakHashMap<Window, Entry>()

    @Synchronized
    fun acquire(window: Window): Float {
        val current = entries[window]
        if (current != null) {
            current.holders += 1
            return current.original
        }
        return window.attributes.screenBrightness.also { original ->
            entries[window] = Entry(original = original, holders = 1)
        }
    }

    @Synchronized
    fun release(window: Window) {
        val current = entries[window] ?: return
        current.holders -= 1
        if (current.holders <= 0) entries.remove(window)
    }
}

@Composable
private fun VideoGestureFeedback(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(color = Color.Black.copy(alpha = 0.72f), shape = RoundedCornerShape(8.dp)) {
            Text(message, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
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

@Composable
private fun PoolNavigationSection(
    postId: Long,
    pools: List<PoolNavigationInfo>,
    onPoolClick: (PoolNavigationInfo) -> Unit,
    onPostClick: (PoolNavigationInfo, Int, Int) -> Unit,
) {
    pools.forEach { pool ->
        val index = pool.postIds.indexOf(postId)
        if (index < 0) return@forEach
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            TextButton(onClick = { onPoolClick(pool) }) {
                Text(pool.name, color = MaterialTheme.colorScheme.primary)
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    enabled = index > 0,
                    onClick = { onPostClick(pool, 0, -1) },
                ) { Text(stringResource(R.string.pool_first)) }
                TextButton(
                    enabled = index > 0,
                    onClick = { onPostClick(pool, index - 1, -1) },
                ) { Text(stringResource(R.string.pool_previous)) }
                TextButton(
                    enabled = index < pool.postIds.lastIndex,
                    onClick = { onPostClick(pool, index + 1, 1) },
                ) { Text(stringResource(R.string.pool_next)) }
                TextButton(
                    enabled = index < pool.postIds.lastIndex,
                    onClick = { onPostClick(pool, pool.postIds.lastIndex, 1) },
                ) { Text(stringResource(R.string.pool_last)) }
            }
        }
    }
}

@Composable
private fun BasicInfo(
    post: PostDetail,
    relatedPreviews: Map<Long, MediaPreview>,
    relatedPreviewsLoaded: Boolean,
    onRelatedPostClick: (Long) -> Unit,
) {
    SectionTitle(R.string.basic_information)
    post.parentId?.let { parentId ->
        RelatedPreviewGroup(
            title = stringResource(R.string.parent_post),
            ids = listOf(parentId),
            previews = relatedPreviews,
            loaded = relatedPreviewsLoaded,
            onPostClick = onRelatedPostClick,
        )
    }
    if (post.childIds.isNotEmpty()) {
        RelatedPreviewGroup(
            title = stringResource(R.string.child_posts),
            ids = post.childIds,
            previews = relatedPreviews,
            loaded = relatedPreviewsLoaded,
            onPostClick = onRelatedPostClick,
        )
    }
    InfoRow(R.string.format, post.extension.uppercase())
    InfoRow(R.string.dimensions, stringResource(R.string.dimensions_value, post.width, post.height))
    InfoRow(R.string.file_size, formatBytes(post.fileSize))
    if (post.md5.isNotBlank()) InfoRow(R.string.md5, post.md5)
    post.durationSeconds?.let { InfoRow(R.string.duration, formatDurationSeconds(it)) }
    InfoRow(R.string.score_breakdown, stringResource(R.string.score_breakdown_value, post.upScore, post.downScore))
    InfoRow(R.string.comment_count, post.commentCount.toString())
    InfoRow(R.string.uploader, stringResource(R.string.user_with_id, post.uploaderName, post.uploaderId))
    InfoRow(R.string.uploaded_at, post.createdAt)
    if (post.sources.isNotEmpty()) SourceLinks(post.sources)
    InfoRow(R.string.has_notes, stringResource(if (post.hasNotes) R.string.yes else R.string.no))
    if (post.statusFlags.isNotEmpty()) InfoRow(R.string.status_flags, statusFlagsText(post.statusFlags))
    if (post.description.isNotBlank()) InfoRow(R.string.description, post.description)
}

@Composable
private fun RelatedPreviewGroup(
    title: String,
    ids: List<Long>,
    previews: Map<Long, MediaPreview>,
    loaded: Boolean,
    onPostClick: (Long) -> Unit,
) {
    val visibleIds = if (loaded) ids.filter(previews::containsKey) else ids
    if (visibleIds.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        RelatedPreviewGrid(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            visibleIds.forEach { id ->
                val preview = previews[id]
                if (preview == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                } else {
                    MediaPreviewCard(
                        item = preview,
                        onClick = { onPostClick(id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RelatedPreviewGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val spacing = 4.dp.roundToPx()
        val columnWidth = ((constraints.maxWidth - spacing).coerceAtLeast(0)) / 2
        val childConstraints = constraints.copy(
            minWidth = columnWidth,
            maxWidth = columnWidth,
            minHeight = 0,
        )
        val columnHeights = IntArray(2)
        val placements = measurables.map { measurable ->
            val column = if (columnHeights[0] <= columnHeights[1]) 0 else 1
            val placeable = measurable.measure(childConstraints)
            val x = column * (columnWidth + spacing)
            val y = columnHeights[column]
            columnHeights[column] += placeable.height
            Triple(placeable, x, y)
        }
        layout(constraints.maxWidth, columnHeights.maxOrNull() ?: 0) {
            placements.forEach { (placeable, x, y) -> placeable.placeRelative(x, y) }
        }
    }
}

internal fun formatDurationSeconds(seconds: Float): String {
    val roundedSeconds = seconds
        .takeIf(Float::isFinite)
        ?.coerceAtLeast(0f)
        ?.let { value -> kotlin.math.floor(value.toDouble() + 0.5).toLong() }
        ?: 0L
    return formatMinutesSeconds(roundedSeconds * 1_000L)
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

@Composable private fun CommentsSection(comments: List<PostComment>, isLoading: Boolean, account: Account?, isActing: Boolean, onRequireLogin: () -> Unit, onCreate: (String) -> Unit, onUpdate: (Long, String) -> Unit, onHide: (Long) -> Unit) {
    var body by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<PostComment?>(null) }
    SectionTitle(R.string.comments)
    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(stringResource(R.string.comment_hint)) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    Button(onClick = { if (account == null) onRequireLogin() else { onCreate(body); body = "" } }, enabled = !isActing && body.isNotBlank(), modifier = Modifier.padding(16.dp)) { Text(stringResource(R.string.post_comment)) }
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp), strokeWidth = 2.dp)
    } else if (comments.isEmpty()) {
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
