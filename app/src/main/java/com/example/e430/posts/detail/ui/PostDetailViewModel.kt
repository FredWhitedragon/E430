package com.example.e430.posts.detail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.core.network.E621Site
import com.example.e430.posts.detail.data.PostDetailRepository
import com.example.e430.posts.detail.model.PostComment
import com.example.e430.posts.detail.model.PostDetail
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
)

class PostDetailViewModel(private val repository: PostDetailRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()
    private var site = E621Site.E621
    private var postId = 0L
    private var loadJob: Job? = null

    fun show(site: E621Site, postId: Long) {
        if (this.site == site && this.postId == postId && _uiState.value.post != null) return
        this.site = site
        this.postId = postId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PostDetailUiState()
            try {
                val post = repository.getPost(site, postId)
                _uiState.value = PostDetailUiState(post = post, isLoading = false)
                val comments = repository.getComments(site, postId)
                _uiState.update { it.copy(comments = comments) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = it.post == null) }
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
            }
        }
    }

    companion object {
        fun factory(repository: PostDetailRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PostDetailViewModel(repository) as T
            }
    }
}
