package com.example.e430.pools.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.core.network.E621Site
import com.example.e430.pools.data.PoolRepository
import com.example.e430.pools.model.PoolDetail
import com.example.e430.posts.detail.data.PostDetailRepository
import com.example.e430.posts.detail.model.PostDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PoolDetailUiState(
    val pool: PoolDetail? = null,
    val posts: Map<Long, PostDetail> = emptyMap(),
    val loadingPostIds: Set<Long> = emptySet(),
    val failedPostIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val visiblePostIndex: Int = 0,
)

class PoolDetailViewModel(
    private val poolRepository: PoolRepository,
    private val postDetailRepository: PostDetailRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PoolDetailUiState())
    val uiState: StateFlow<PoolDetailUiState> = _uiState.asStateFlow()

    private val _membershipPools = MutableStateFlow<List<PoolDetail>>(emptyList())
    val membershipPools: StateFlow<List<PoolDetail>> = _membershipPools.asStateFlow()

    private var site = E621Site.E621
    private var poolId = 0L
    private var poolJob: Job? = null
    private var windowJob: Job? = null
    private var membershipJob: Job? = null
    private var membershipKey: Pair<E621Site, List<Long>>? = null
    private var windowIds: Set<Long> = emptySet()

    fun show(site: E621Site, poolId: Long) {
        if (this.site == site && this.poolId == poolId && _uiState.value.pool != null) return
        this.site = site
        this.poolId = poolId
        poolJob?.cancel()
        windowJob?.cancel()
        windowIds = emptySet()
        _uiState.value = PoolDetailUiState()
        poolJob = viewModelScope.launch {
            try {
                val pool = poolRepository.getPool(site, poolId)
                if (this@PoolDetailViewModel.site == site && this@PoolDetailViewModel.poolId == poolId) {
                    _uiState.value = PoolDetailUiState(pool = pool, isLoading = false)
                    loadWindow(0)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun loadWindow(index: Int) {
        val pool = _uiState.value.pool ?: return
        val targetIds = pool.postIds.drop(index.coerceAtLeast(0)).take(WINDOW_SIZE).toSet()
        if (targetIds.isEmpty()) return
        if (targetIds == windowIds && _uiState.value.failedPostIds.isEmpty()) return
        windowIds = targetIds
        windowJob?.cancel()
        _uiState.update { state ->
            state.copy(
                visiblePostIndex = index.coerceIn(0, pool.postIds.lastIndex),
                posts = state.posts.filterKeys(targetIds::contains),
                loadingPostIds = targetIds - state.posts.keys,
                failedPostIds = emptySet(),
            )
        }
        windowJob = viewModelScope.launch {
            coroutineScope {
                targetIds.map { id ->
                    async {
                        runCatching { id to postDetailRepository.getPost(site, id) }
                            .onSuccess { (loadedId, post) ->
                                _uiState.update { state ->
                                    if (loadedId !in targetIds) state else state.copy(
                                        posts = state.posts + (loadedId to post),
                                        loadingPostIds = state.loadingPostIds - loadedId,
                                    )
                                }
                            }
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                _uiState.update { state ->
                                    state.copy(
                                        loadingPostIds = state.loadingPostIds - id,
                                        failedPostIds = state.failedPostIds + id,
                                    )
                                }
                            }
                    }
                }.awaitAll()
            }
        }
    }

    fun loadMembershipPools(site: E621Site, ids: List<Long>) {
        val key = site to ids.distinct()
        if (key == membershipKey) return
        membershipKey = key
        membershipJob?.cancel()
        if (ids.isEmpty()) {
            _membershipPools.value = emptyList()
            return
        }
        membershipJob = viewModelScope.launch {
            try {
                _membershipPools.value = poolRepository.getPoolsByIds(site, ids)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _membershipPools.value = emptyList()
            }
        }
    }

    fun retry() {
        val retryId = poolId
        poolId = 0L
        show(site, retryId)
    }

    companion object {
        private const val WINDOW_SIZE = 2

        fun factory(
            poolRepository: PoolRepository,
            postDetailRepository: PostDetailRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PoolDetailViewModel(poolRepository, postDetailRepository) as T
        }
    }
}
