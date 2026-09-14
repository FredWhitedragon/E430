package com.example.e430.pools.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.core.network.E621Site
import com.example.e430.pools.data.PoolRepository
import com.example.e430.pools.model.PoolPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PoolGridRequest(val site: E621Site, val query: String)

data class PoolGridUiState(
    val items: List<PoolPreview> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    val endReached: Boolean = false,
)

class PoolGridViewModel(
    private val repository: PoolRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PoolGridUiState())
    val uiState: StateFlow<PoolGridUiState> = _uiState.asStateFlow()

    private var activeRequest: PoolGridRequest? = null
    private var loadJob: Job? = null
    private var currentPage = 1

    fun show(request: PoolGridRequest) {
        if (request == activeRequest && _uiState.value.items.isNotEmpty()) return
        activeRequest = request
        currentPage = 1
        _uiState.value = PoolGridUiState()
        loadFirstPage(refresh = false)
    }

    fun refresh() = loadFirstPage(refresh = true)

    fun loadMore() {
        val state = _uiState.value
        val request = activeRequest ?: return
        if (state.isInitialLoading || state.isRefreshing || state.isLoadingMore || state.endReached) return
        val nextPage = currentPage + 1
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
            try {
                val page = repository.getPools(request.site, request.query, nextPage)
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
                val page = repository.getPools(request.site, request.query, page = 1)
                if (request == activeRequest) {
                    currentPage = 1
                    _uiState.value = PoolGridUiState(
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
        fun factory(repository: PoolRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PoolGridViewModel(repository) as T
                }
            }
    }
}
