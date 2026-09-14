package com.example.e430.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.posts.model.HomeSort
import com.example.e430.settings.data.SettingsRepository
import com.example.e430.settings.model.ImageQualityPreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val homeSort: StateFlow<HomeSort> = repository.homeSort.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeSort.Latest,
    )
    val imageQuality = repository.imageQuality.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ImageQualityPreference.Auto,
    )
    val videoAutoPlay = repository.videoAutoPlay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val videoMuted = repository.videoMuted.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val tagsCollapsed = repository.tagsCollapsed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val downloadDirectory = repository.downloadDirectory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "E430")
    private val _customHomeQuery = MutableStateFlow("")
    val customHomeQuery: StateFlow<String> = _customHomeQuery.asStateFlow()
    private var customHomeQueryLoaded = false
    private var customHomeQueryWriteJob: Job? = null

    init {
        viewModelScope.launch {
            repository.customHomeQuery.collect { storedQuery ->
                if (!customHomeQueryLoaded) {
                    _customHomeQuery.value = storedQuery
                    customHomeQueryLoaded = true
                }
            }
        }
    }

    fun setHomeSort(sort: HomeSort) {
        viewModelScope.launch { repository.setHomeSort(sort) }
    }

    fun setCustomHomeQuery(query: String) {
        customHomeQueryLoaded = true
        _customHomeQuery.value = query
        customHomeQueryWriteJob?.cancel()
        customHomeQueryWriteJob = viewModelScope.launch {
            repository.setCustomHomeQuery(query)
        }
    }

    fun setImageQuality(value: ImageQualityPreference) = launch { repository.setImageQuality(value) }
    fun setVideoAutoPlay(value: Boolean) = launch { repository.setVideoAutoPlay(value) }
    fun setVideoMuted(value: Boolean) = launch { repository.setVideoMuted(value) }
    fun setTagsCollapsed(value: Boolean) = launch { repository.setTagsCollapsed(value) }
    fun setDownloadDirectory(value: String) = launch { repository.setDownloadDirectory(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
