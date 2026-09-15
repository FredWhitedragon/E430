package com.example.e430.posts.detail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.core.network.E621Site
import com.example.e430.posts.detail.data.PostDetailRepository
import com.example.e430.posts.detail.model.PostComment
import com.example.e430.posts.detail.model.PostDetail
import com.example.e430.posts.detail.model.MediaQuality
import com.example.e430.posts.data.PostRepository
import com.example.e430.posts.model.MediaPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostDetailUiState(
    val post: PostDetail? = null,
    val comments: List<PostComment> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isActing: Boolean = false,
    val actionFailed: Boolean = false,
    val isCommentsLoading: Boolean = true,
    val relatedPreviews: Map<Long, MediaPreview> = emptyMap(),
    val relatedPreviewsLoaded: Boolean = false,
)

class PostDetailViewModel(
    private val repository: PostDetailRepository,
    private val postRepository: PostRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()
    private var site = E621Site.E621
    private var postId = 0L
    private var loadJob: Job? = null
    private var prefetchJob: Job? = null
    private var relatedJob: Job? = null

    fun cachedState(site: E621Site, postId: Long): PostDetailUiState? =
        repository.cachedDetail(site, postId)?.let { cached ->
            PostDetailUiState(
                post = cached.post,
                comments = cached.comments,
                isLoading = false,
                isCommentsLoading = !cached.commentsLoaded,
            )
        }

    fun show(site: E621Site, postId: Long) {
        if (this.site == site && this.postId == postId && _uiState.value.post != null) return
        this.site = site
        this.postId = postId
        loadJob?.cancel()
        cachedState(site, postId)?.let { cached ->
            _uiState.value = cached
            cached.post?.let(::loadRelated)
            if (!cached.isCommentsLoading) return
            loadJob = viewModelScope.launch {
                try {
                    val comments = repository.getComments(site, postId)
                    _uiState.update { state ->
                        if (state.post?.id == postId) {
                            state.copy(comments = comments, isCommentsLoading = false)
                        } else state
                    }
                    cached.post?.let { repository.cacheSnapshot(site, it, comments) }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    _uiState.update { state ->
                        if (state.post?.id == postId) state.copy(isCommentsLoading = false) else state
                    }
                }
            }
            return
        }
        loadJob = viewModelScope.launch {
            _uiState.value = PostDetailUiState()
            try {
                val cached = repository.cachedDetail(site, postId)
                if (cached != null) {
                    _uiState.value = PostDetailUiState(
                        post = cached.post,
                        comments = cached.comments,
                        isLoading = false,
                        isCommentsLoading = !cached.commentsLoaded,
                    )
                    loadRelated(cached.post)
                    if (!cached.commentsLoaded) {
                        val comments = repository.getComments(site, postId)
                        _uiState.update { state ->
                            if (state.post?.id == postId) {
                                state.copy(comments = comments, isCommentsLoading = false)
                            } else state
                        }
                        repository.cacheSnapshot(site, cached.post, comments)
                    }
                } else {
                    val post = repository.getPost(site, postId)
                    _uiState.value = PostDetailUiState(post = post, isLoading = false)
                    loadRelated(post)
                    val comments = repository.getComments(site, postId)
                    _uiState.update { state ->
                        if (state.post?.id == postId) {
                            state.copy(comments = comments, isCommentsLoading = false)
                        } else {
                            state
                        }
                    }
                    repository.cacheSnapshot(site, post, comments)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = it.post == null,
                        isCommentsLoading = false,
                    )
                }
            }
        }
    }

    fun retry() {
        val retrySite = site
        val retryPostId = postId
        this.postId = 0
        show(retrySite, retryPostId)
    }

    fun vote(value: Int) {
        val actionSite = site
        val post = _uiState.value.post ?: return
        act {
        if (post.userVote == value) {
            repository.removeVote(actionSite, post.id)
            _uiState.update { if (it.post?.id == post.id) it.copy(post = post.copy(score = post.score - value, userVote = 0)) else it }
        } else {
            val response = repository.vote(actionSite, post.id, value)
            _uiState.update {
                if (it.post?.id != post.id) it else it.copy(post = post.copy(
                    score = response.score,
                    upScore = response.up,
                    downScore = response.down,
                    userVote = response.ourScore,
                ))
            }
        }
        }
    }

    fun toggleFavorite() {
        val actionSite = site
        val post = _uiState.value.post ?: return
        act {
        val response = if (post.isFavorited) repository.removeFavorite(actionSite, post.id)
        else repository.addFavorite(actionSite, post.id)
        _uiState.update {
            if (it.post?.id != post.id) it else it.copy(post = post.copy(
                favoriteCount = response.favoriteCount,
                isFavorited = !post.isFavorited,
            ))
        }
        }
    }

    fun createComment(body: String) {
        if (body.isBlank()) return
        val actionSite = site
        val actionPostId = postId
        act {
        val comment = repository.createComment(actionSite, actionPostId, body.trim())
        _uiState.update { state ->
            if (state.post?.id != actionPostId) state else state.copy(
                comments = state.comments + comment,
                post = state.post?.copy(commentCount = state.post.commentCount + 1),
            )
        }
        }
    }

    fun updateComment(id: Long, body: String) {
        if (body.isBlank()) return
        val actionSite = site
        val actionPostId = postId
        act {
        repository.updateComment(actionSite, id, body.trim())
        _uiState.update { state ->
            if (state.post?.id != actionPostId) state else state.copy(comments = state.comments.map { if (it.id == id) it.copy(body = body.trim()) else it })
        }
        }
    }

    fun hideComment(id: Long) {
        val actionSite = site
        val actionPostId = postId
        act {
        val updated = repository.hideComment(actionSite, id)
        _uiState.update { state ->
            if (state.post?.id != actionPostId) state else state.copy(comments = state.comments.map { if (it.id == id) updated else it })
        }
        }
    }

    fun clearActionError() = _uiState.update { it.copy(actionFailed = false) }

    fun prefetch(
        site: E621Site,
        postIds: List<Long>,
        imageQuality: MediaQuality,
        preloadImages: Boolean,
    ) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            postIds.distinct().take(4).forEach { id ->
                try {
                    repository.prefetch(site, id, imageQuality, preloadImages)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // Prefetch failures must not replace the visible detail state.
                }
            }
        }
    }

    fun clearCache() {
        loadJob?.cancel()
        prefetchJob?.cancel()
        relatedJob?.cancel()
        _uiState.value = PostDetailUiState()
        repository.clearCache()
    }

    private fun loadRelated(post: PostDetail) {
        relatedJob?.cancel()
        val ids = listOfNotNull(post.parentId) + post.childIds
        if (ids.isEmpty()) {
            _uiState.update { state ->
                if (state.post?.id == post.id) state.copy(relatedPreviewsLoaded = true) else state
            }
            return
        }
        val requestSite = site
        relatedJob = viewModelScope.launch {
            try {
                val previews = postRepository.getPostsByIds(requestSite, ids)
                _uiState.update { state ->
                    if (state.post?.id == post.id) {
                        state.copy(relatedPreviews = previews, relatedPreviewsLoaded = true)
                    } else state
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { state ->
                    if (state.post?.id == post.id) state.copy(relatedPreviewsLoaded = true) else state
                }
            }
        }
    }

    private fun act(block: suspend () -> Unit) {
        if (_uiState.value.isActing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActing = true, actionFailed = false) }
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(actionFailed = true) }
            } finally {
                _uiState.update { it.copy(isActing = false) }
                val snapshot = _uiState.value
                snapshot.post?.let { repository.cacheSnapshot(site, it, snapshot.comments) }
            }
        }
    }

    companion object {
        fun factory(
            repository: PostDetailRepository,
            postRepository: PostRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PostDetailViewModel(repository, postRepository) as T
            }
    }
}
