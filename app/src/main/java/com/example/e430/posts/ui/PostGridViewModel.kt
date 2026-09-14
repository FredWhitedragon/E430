package com.example.e430.posts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.core.network.E621Site
import com.example.e430.posts.data.PostRepository
import com.example.e430.posts.model.HomeSort
import com.example.e430.posts.model.MediaPreview
import com.example.e430.posts.model.PostFeed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostGridRequest(
    val site: E621Site,
    val feed: PostFeed,
    val homeSort: HomeSort,
    val query: String,
    val blacklist: String = "",
)

data class PostGridUiState(
    val items: List<MediaPreview> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    val endReached: Boolean = false,
)

class PostGridViewModel(
    private val repository: PostRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostGridUiState())
    val uiState: StateFlow<PostGridUiState> = _uiState.asStateFlow()

    private var activeRequest: PostGridRequest? = null
    private var loadJob: Job? = null
    private var currentPage = 1

    fun show(request: PostGridRequest) {
        if (request == activeRequest && _uiState.value.items.isNotEmpty()) return
        activeRequest = request
        currentPage = 1
        _uiState.value = PostGridUiState()
        loadFirstPage(refresh = false)
    }

    fun refresh() = loadFirstPage(refresh = true)

    fun clear() {
        loadJob?.cancel()
        activeRequest = null
        currentPage = 1
        _uiState.value = PostGridUiState()
    }

    fun loadMore() {
        val state = _uiState.value
        val request = activeRequest ?: return
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMore || state.endReached) return
        val nextPage = currentPage + 1
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
            try {
                val page = repository.getPosts(
                    site = request.site,
                    feed = request.feed,
                    homeSort = request.homeSort,
                    query = request.query,
                    page = nextPage,
                    blacklist = request.blacklist,
                )
                if (request == activeRequest) {
                    currentPage = nextPage
                    _uiState.update { current ->
                        current.copy(
                            items = (current.items + page.items).distinctBy { it.id },
                            isLoadingMore = false,
                            endReached = page.endReached,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (request == activeRequest) {
                    _uiState.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
                }
            }
        }
    }

    private fun loadFirstPage(refresh: Boolean) {
        val request = activeRequest ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInitialLoading = !refresh && it.items.isEmpty(),
                    isRefreshing = refresh,
                    hasError = false,
                )
            }
            try {
                val page = repository.getPosts(
                    site = request.site,
                    feed = request.feed,
                    homeSort = request.homeSort,
                    query = request.query,
                    page = 1,
                    blacklist = request.blacklist,
                )
                if (request == activeRequest) {
                    currentPage = 1
                    _uiState.value = PostGridUiState(
                        items = page.items,
                        isInitialLoading = false,
                        endReached = page.endReached,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (request == activeRequest) {
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            hasError = true,
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(repository: PostRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PostGridViewModel(repository) as T
                }
            }
    }
}
