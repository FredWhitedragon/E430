package com.example.e430.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.core.network.E621Site
import com.example.e430.search.data.TagSuggestionRepository
import com.example.e430.search.model.TagSuggestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchSuggestionUiState(
    val suggestions: List<TagSuggestion> = emptyList(),
)

class SearchSuggestionViewModel(
    private val repository: TagSuggestionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchSuggestionUiState())
    val uiState: StateFlow<SearchSuggestionUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null
    private var requestRevision = 0L

    fun request(site: E621Site, prefix: String, useLocalMetaTags: Boolean) {
        val revision = ++requestRevision
        requestJob?.cancel()
        _uiState.value = SearchSuggestionUiState()
        requestJob = viewModelScope.launch {
            delay(INPUT_SETTLE_MILLIS)
            val suggestions = try {
                if (useLocalMetaTags) {
                    localMetaTagSuggestions(prefix)
                } else {
                    repository.suggest(site, prefix)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            if (revision == requestRevision) {
                _uiState.value = SearchSuggestionUiState(suggestions)
            }
        }
    }

    fun clear() {
        requestRevision += 1
        requestJob?.cancel()
        requestJob = null
        _uiState.value = SearchSuggestionUiState()
    }

    override fun onCleared() {
        requestJob?.cancel()
    }

    companion object {
        private const val INPUT_SETTLE_MILLIS = 500L
        private const val META_CATEGORY = 7

        private val metaTagValues = listOf(
            "rating:s",
            "rating:q",
            "rating:e",
            "order:id",
            "order:id_asc",
            "order:favcount",
            "order:favcount_asc",
            "order:score",
            "order:score_asc",
            "order:comment_count",
            "order:comment_count_asc",
            "status:active",
            "status:pending",
            "status:flagged",
            "status:deleted",
            "status:any",
        )

        internal fun localMetaTagSuggestions(prefix: String): List<TagSuggestion> =
            metaTagValues
                .asSequence()
                .filter { it.startsWith(prefix, ignoreCase = true) }
                .map { TagSuggestion(name = it, category = META_CATEGORY) }
                .toList()

        fun factory(repository: TagSuggestionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchSuggestionViewModel(repository) as T
                }
            }
    }
}
