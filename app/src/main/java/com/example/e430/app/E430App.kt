package com.example.e430.app

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.e430.R
import com.example.e430.account.model.Account
import com.example.e430.account.ui.AccountAvatar
import com.example.e430.account.ui.AccountScreen
import com.example.e430.account.ui.AccountViewModel
import com.example.e430.account.ui.LoginDialog
import com.example.e430.core.network.E621Site
import com.example.e430.core.ui.theme.E430Blue
import com.example.e430.core.ui.theme.E430Gold
import com.example.e430.core.ui.theme.E430Theme
import com.example.e430.pools.ui.PoolGridRequest
import com.example.e430.pools.ui.PoolGridRoute
import com.example.e430.pools.ui.PoolGridViewModel
import com.example.e430.posts.model.HomeSort
import com.example.e430.posts.model.PostFeed
import com.example.e430.posts.ui.PostGridRequest
import com.example.e430.posts.ui.PostGridRoute
import com.example.e430.posts.ui.PostGridViewModel
import com.example.e430.posts.detail.model.MediaSource
import com.example.e430.posts.detail.model.PostDetail
import com.example.e430.posts.detail.ui.PostDetailRoute
import com.example.e430.posts.detail.ui.PostDetailViewModel
import com.example.e430.search.ui.SearchHeader
import com.example.e430.settings.ui.SettingsScreen
import com.example.e430.settings.ui.SettingsViewModel
import kotlinx.coroutines.launch

private enum class Destination(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Home(R.string.home, R.drawable.ic_home),
    Latest(R.string.latest, R.drawable.ic_newest),
    Popular(R.string.popular, R.drawable.ic_hottest),
    Favorites(R.string.favorites, R.drawable.ic_favorite),
    Pools(R.string.pools, R.drawable.ic_pools),
}

@Composable
fun E430App() {
    val context = LocalContext.current
    val container = remember { AppContainer(context.applicationContext) }
    val systemDarkTheme = isSystemInDarkTheme()
    var darkThemeOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val useDarkTheme = darkThemeOverride ?: systemDarkTheme

    E430Theme(darkTheme = useDarkTheme) {
        E430Home(
            container = container,
            useDarkTheme = useDarkTheme,
            onDarkThemeChange = { darkThemeOverride = it },
        )
    }
}

@Composable
private fun E430Home(
    container: AppContainer,
    useDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val postViewModel: PostGridViewModel = viewModel(
        factory = PostGridViewModel.factory(container.postRepository),
    )
    val tagPostViewModel: PostGridViewModel = viewModel(
        key = "tag_posts",
        factory = PostGridViewModel.factory(container.postRepository),
    )
    val postDetailViewModel: PostDetailViewModel = viewModel(
        factory = PostDetailViewModel.factory(container.postDetailRepository),
    )
    val mainPostState by postViewModel.uiState.collectAsStateWithLifecycle()
    val tagPostState by tagPostViewModel.uiState.collectAsStateWithLifecycle()
    val poolViewModel: PoolGridViewModel = viewModel(
        factory = PoolGridViewModel.factory(container.poolRepository),
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.settingsRepository),
    )
    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.factory(container.accountRepository),
    )
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val homeSort by settingsViewModel.homeSort.collectAsStateWithLifecycle()
    val customHomeQuery by settingsViewModel.customHomeQuery.collectAsStateWithLifecycle()
    val imageQuality by settingsViewModel.imageQuality.collectAsStateWithLifecycle()
    val videoAutoPlay by settingsViewModel.videoAutoPlay.collectAsStateWithLifecycle()
    val videoMuted by settingsViewModel.videoMuted.collectAsStateWithLifecycle()
    val tagsCollapsed by settingsViewModel.tagsCollapsed.collectAsStateWithLifecycle()
    val downloadDirectory by settingsViewModel.downloadDirectory.collectAsStateWithLifecycle()
    var selectedDestinationName by rememberSaveable { mutableStateOf(Destination.Home.name) }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var accountVisible by rememberSaveable { mutableStateOf(false) }
    var loginDialogVisible by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    var filtersVisible by rememberSaveable { mutableStateOf(false) }
    var useE926 by rememberSaveable { mutableStateOf(false) }
    var originalDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var currentDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var activeTag by rememberSaveable { mutableStateOf<String?>(null) }
    var detailSequence by rememberSaveable { mutableStateOf(longArrayOf()) }
    var detailSiteName by rememberSaveable { mutableStateOf(E621Site.E621.name) }
    var returnFocusPostId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailSlideDirection by rememberSaveable { mutableStateOf(0) }
    var pendingDownload by remember { mutableStateOf<Pair<PostDetail, MediaSource>?>(null) }
    val destination = Destination.entries.firstOrNull {
        it.name == selectedDestinationName
    } ?: Destination.Home
    val site = if (useE926) E621Site.E926 else E621Site.E621
    val shareTitle = stringResource(R.string.share)
    val safeDownloadDirectory = container.mediaFileRepository.sanitizeFolder(downloadDirectory)
    val activeBlacklist = accountState.account?.blacklistedTags.orEmpty()
    val homeQuery = if (destination == Destination.Home && homeSort == HomeSort.Custom) {
        listOf(customHomeQuery.trim(), submittedQuery.trim())
            .filter(String::isNotEmpty)
            .joinToString(" ")
    } else {
        submittedQuery
    }
    fun startDownload(post: PostDetail, source: MediaSource) {
        val success = container.mediaFileRepository.enqueueDownload(
            context = context,
            post = post,
            source = source,
            directory = safeDownloadDirectory,
        )
        Toast.makeText(
            context,
            if (success) R.string.download_started else R.string.download_failed,
            Toast.LENGTH_LONG,
        ).show()
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingDownload?.let { (post, source) ->
            if (granted) startDownload(post, source)
            else Toast.makeText(context, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
        }
        pendingDownload = null
    }

    LaunchedEffect(accountState.account) {
        accountState.account?.let { account ->
            loginDialogVisible = false
            useE926 = account.site == E621Site.E926
        }
    }
    LaunchedEffect(accountVisible, accountState.isRestoring, accountState.account) {
        if (accountVisible && !accountState.isRestoring && accountState.account == null) {
            accountVisible = false
            loginDialogVisible = true
        }
    }

    BackHandler(enabled = settingsVisible || accountVisible) {
        settingsVisible = false
        accountVisible = false
    }
    BackHandler(enabled = currentDetailId != null || activeTag != null) {
        when {
            currentDetailId != null && activeTag != null -> currentDetailId = null
            activeTag != null -> {
                activeTag = null
                currentDetailId = originalDetailId
                detailSequence = longArrayOf(originalDetailId ?: 0L)
            }
            currentDetailId != null -> {
                returnFocusPostId = currentDetailId
                currentDetailId = null
                originalDetailId = null
            }
        }
    }

    if (loginDialogVisible && accountState.account == null) {
        LoginDialog(
            site = site,
            isLoggingIn = accountState.isLoggingIn,
            loginFailed = accountState.loginFailed || accountState.restoreFailed,
            onDismiss = {
                loginDialogVisible = false
                accountViewModel.clearLoginError()
            },
            onLogin = { username, apiKey -> accountViewModel.login(site, username, apiKey) },
            onGetApiKey = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, "https://e621.net/api_keys".toUri()),
                    )
                }
            },
        )
    }

    if (accountVisible) {
        val account = accountState.account
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            SecondaryHeader(
                title = stringResource(R.string.account),
                onBack = { accountVisible = false },
            )
            if (account == null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else {
                AccountScreen(
                    account = account,
                    isSavingBlacklist = accountState.isSavingBlacklist,
                    blacklistSaveFailed = accountState.blacklistSaveFailed,
                    blacklistSaved = accountState.blacklistSaved,
                    onSaveBlacklist = accountViewModel::updateBlacklist,
                    onLogout = {
                        postViewModel.clear()
                        tagPostViewModel.clear()
                        accountViewModel.logout()
                        accountVisible = false
                        if (destination == Destination.Favorites) {
                            selectedDestinationName = Destination.Home.name
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }

    if (settingsVisible) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            SecondaryHeader(
                title = stringResource(R.string.more_settings),
                onBack = { settingsVisible = false },
            )
            SettingsScreen(
                homeSort = homeSort,
                customHomeQuery = customHomeQuery,
                imageQuality = imageQuality,
                videoAutoPlay = videoAutoPlay,
                videoMuted = videoMuted,
                tagsCollapsed = tagsCollapsed,
                downloadDirectory = downloadDirectory,
                onHomeSortChange = settingsViewModel::setHomeSort,
                onCustomHomeQueryChange = settingsViewModel::setCustomHomeQuery,
                onImageQualityChange = settingsViewModel::setImageQuality,
                onVideoAutoPlayChange = settingsViewModel::setVideoAutoPlay,
                onVideoMutedChange = settingsViewModel::setVideoMuted,
                onTagsCollapsedChange = settingsViewModel::setTagsCollapsed,
                onDownloadDirectoryChange = settingsViewModel::setDownloadDirectory,
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    currentDetailId?.let { postId ->
        val detailSite = E621Site.entries.firstOrNull { it.name == detailSiteName } ?: site
        val detailAccount = accountState.account?.takeIf { it.site == detailSite }
        fun moveDetail(offset: Int) {
            val index = detailSequence.indexOf(postId)
            val next = detailSequence.getOrNull(index + offset) ?: return
            detailSlideDirection = offset
            currentDetailId = next
            if (activeTag == null) originalDetailId = next
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            SecondaryHeader(
                title = stringResource(R.string.post_number, postId),
                onBack = {
                    if (activeTag != null) currentDetailId = null
                    else {
                        returnFocusPostId = postId
                        currentDetailId = null
                        originalDetailId = null
                    }
                },
            )
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val density = LocalDensity.current
                val slideOffset = remember { Animatable(0f) }
                LaunchedEffect(postId) {
                    if (detailSlideDirection != 0) {
                        slideOffset.snapTo(
                            detailSlideDirection * with(density) { maxWidth.toPx() },
                        )
                        slideOffset.animateTo(0f, animationSpec = tween(durationMillis = 260))
                    } else {
                        slideOffset.snapTo(0f)
                    }
                }
                PostDetailRoute(
                    site = detailSite,
                    postId = postId,
                    account = detailAccount,
                    imageQualityPreference = imageQuality,
                    videoAutoPlay = videoAutoPlay,
                    videoMuted = videoMuted,
                    tagsCollapsedByDefault = tagsCollapsed,
                    downloadDirectory = safeDownloadDirectory,
                    viewModel = postDetailViewModel,
                    onTagClick = { tag -> activeTag = tag; currentDetailId = null },
                    onPrevious = { moveDetail(-1) },
                    onNext = { moveDetail(1) },
                    onRequireLogin = {
                        Toast.makeText(context, R.string.login_required, Toast.LENGTH_SHORT).show()
                    },
                    onDownload = { post, source ->
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingDownload = post to source
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else startDownload(post, source)
                    },
                    onShare = { post, source ->
                        scope.launch {
                            val uri = container.mediaFileRepository.prepareShare(context, post, source)
                            if (uri == null) {
                                Toast.makeText(context, R.string.share_failed, Toast.LENGTH_LONG).show()
                            } else {
                                val share = Intent(Intent.ACTION_SEND)
                                    .setType(if (post.kind == com.example.e430.posts.detail.model.MediaKind.Video) "video/*" else "image/*")
                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                context.startActivity(Intent.createChooser(share, shareTitle))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = slideOffset.value },
                )
            }
        }
        return
    }

    activeTag?.let { tag ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            SecondaryHeader(
                title = tag,
                onBack = {
                    activeTag = null
                    currentDetailId = originalDetailId
                    detailSequence = longArrayOf(originalDetailId ?: 0L)
                },
            )
            PostGridRoute(
                request = PostGridRequest(
                    site = E621Site.entries.firstOrNull { it.name == detailSiteName } ?: site,
                    feed = PostFeed.Home,
                    homeSort = HomeSort.Latest,
                    query = tag,
                    blacklist = activeBlacklist,
                ),
                viewModel = tagPostViewModel,
                onPostClick = { id ->
                    detailSequence = tagPostState.items.map { it.id }.toLongArray()
                    detailSlideDirection = 0
                    currentDetailId = id
                },
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                selectedDestination = destination,
                account = accountState.account,
                isRestoringAccount = accountState.isRestoring,
                siteName = site.displayName,
                useE926 = useE926,
                useDarkTheme = useDarkTheme,
                onDestinationClick = {
                    selectedDestinationName = it.name
                    scope.launch { drawerState.close() }
                },
                onSiteChange = { useE926 = it },
                onDarkThemeChange = onDarkThemeChange,
                onMoreSettingsClick = {
                    settingsVisible = true
                    scope.launch { drawerState.close() }
                },
                onAccountClick = {
                    if (accountState.account == null) {
                        accountViewModel.clearLoginError()
                        loginDialogVisible = true
                    } else {
                        accountVisible = true
                    }
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            SearchHeader(
                query = query,
                filtersVisible = filtersVisible,
                onQueryChange = { query = it },
                onSearch = {
                    submittedQuery = query.trim()
                    selectedDestinationName = Destination.Home.name
                },
                onMenuClick = { scope.launch { drawerState.open() } },
                onSearchFocusChange = { filtersVisible = it },
            )
            if (accountState.restoreFailed) AccountRestoreErrorBanner()
            if (accountState.account == null && !accountState.isRestoring) AnonymousBanner()
            when (destination) {
                Destination.Home,
                Destination.Latest,
                Destination.Popular,
                -> PostGridRoute(
                    request = PostGridRequest(
                        site = site,
                        feed = when (destination) {
                            Destination.Home -> PostFeed.Home
                            Destination.Latest -> PostFeed.Latest
                            else -> PostFeed.Popular
                        },
                        homeSort = homeSort,
                        query = homeQuery,
                        blacklist = activeBlacklist,
                    ),
                    viewModel = postViewModel,
                    onPostClick = { id ->
                        detailSlideDirection = 0
                        originalDetailId = id
                        currentDetailId = id
                        detailSiteName = site.name
                        detailSequence = mainPostState.items.map { it.id }.toLongArray()
                    },
                    focusPostId = returnFocusPostId,
                    onFocusConsumed = { returnFocusPostId = null },
                    modifier = Modifier.weight(1f),
                )
                Destination.Pools -> PoolGridRoute(
                    request = PoolGridRequest(site = site, query = submittedQuery),
                    viewModel = poolViewModel,
                    modifier = Modifier.weight(1f),
                )
                Destination.Favorites -> {
                    val account = accountState.account
                    if (account == null) {
                        SignInRequired(modifier = Modifier.weight(1f))
                    } else {
                        PostGridRoute(
                            request = PostGridRequest(
                                site = account.site,
                                feed = PostFeed.Favorites,
                                homeSort = HomeSort.Latest,
                                query = "",
                                blacklist = activeBlacklist,
                            ),
                            viewModel = postViewModel,
                            onPostClick = { id ->
                                detailSlideDirection = 0
                                originalDetailId = id
                                currentDetailId = id
                                detailSiteName = account.site.name
                                detailSequence = mainPostState.items.map { it.id }.toLongArray()
                            },
                            focusPostId = returnFocusPostId,
                            onFocusConsumed = { returnFocusPostId = null },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryHeader(title: String, onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun AccountRestoreErrorBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.restore_sign_in_failed),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun AnonymousBanner() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.anonymous_banner),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun SignInRequired(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.favorites_sign_in),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppDrawer(
    selectedDestination: Destination,
    account: Account?,
    isRestoringAccount: Boolean,
    siteName: String,
    useE926: Boolean,
    useDarkTheme: Boolean,
    onDestinationClick: (Destination) -> Unit,
    onSiteChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onMoreSettingsClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    ModalDrawerSheet {
        AccountHeader(
            account = account,
            siteName = siteName,
            isRestoring = isRestoringAccount,
            onClick = onAccountClick,
        )
        Text(
            text = stringResource(R.string.browse),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 8.dp),
        )
        Destination.entries.take(5).forEach { destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.label)) },
                selected = destination == selectedDestination,
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = null,
                    )
                },
                onClick = { onDestinationClick(destination) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = E430Gold.copy(alpha = 0.24f),
                    selectedIconColor = E430Blue,
                    selectedTextColor = E430Blue,
                ),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        Text(
            text = stringResource(R.string.quick_settings),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 8.dp),
        )
        SettingSwitchRow(
            title = stringResource(R.string.default_site),
            detail = siteName,
            checked = useE926,
            onCheckedChange = onSiteChange,
        )
        SettingSwitchRow(
            title = stringResource(R.string.dark_theme),
            checked = useDarkTheme,
            onCheckedChange = onDarkThemeChange,
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.more_settings)) },
            selected = false,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
            },
            badge = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = onMoreSettingsClick,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun AccountHeader(
    account: Account?,
    siteName: String,
    isRestoring: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = E430Blue,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isRestoring, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .statusBarsPadding()
                .padding(24.dp),
        ) {
            AccountAvatar(account = account, size = 58)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = when {
                        isRestoring -> stringResource(R.string.restoring_account)
                        account != null -> account.username
                        else -> stringResource(R.string.not_signed_in)
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.browsing_site,
                        account?.site?.displayName ?: siteName,
                    ),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    detail: String? = null,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 28.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
