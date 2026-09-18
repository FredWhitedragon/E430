package com.example.e430.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.posts.model.HomeSort
import com.example.e430.settings.data.SettingsRepository
import com.example.e430.settings.model.ImageQualityPreference
import com.example.e430.settings.model.VideoGestureSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val homeSort: StateFlow<HomeSort> = repository.homeSort.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeSort.Latest,
    )
    val meteredImageQuality = repository.meteredImageQuality.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ImageQualityPreference.Low,
    )
    val wifiImageQuality = repository.wifiImageQuality.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ImageQualityPreference.Medium,
    )
    val videoAutoPlay = repository.videoAutoPlay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val videoMuted = repository.videoMuted.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val tagsCollapsed = repository.tagsCollapsed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val downloadDirectory = repository.downloadDirectory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "E430")
    val prefetchOnMetered = repository.prefetchOnMetered.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val videoLoop = repository.videoLoop.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val videoGestureSettings = repository.videoGestureSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        VideoGestureSettings(),
    )
    fun setHomeSort(sort: HomeSort) {
        viewModelScope.launch { repository.setHomeSort(sort) }
    }

    fun setMeteredImageQuality(value: ImageQualityPreference) = launch { repository.setMeteredImageQuality(value) }
    fun setWifiImageQuality(value: ImageQualityPreference) = launch { repository.setWifiImageQuality(value) }
    fun setVideoAutoPlay(value: Boolean) = launch { repository.setVideoAutoPlay(value) }
    fun setVideoMuted(value: Boolean) = launch { repository.setVideoMuted(value) }
    fun setTagsCollapsed(value: Boolean) = launch { repository.setTagsCollapsed(value) }
    fun setDownloadDirectory(value: String) = launch { repository.setDownloadDirectory(value) }
    fun setPrefetchOnMetered(value: Boolean) = launch { repository.setPrefetchOnMetered(value) }
    fun setVideoLoop(value: Boolean) = launch { repository.setVideoLoop(value) }
    fun setVideoDoubleTapPlayPause(value: Boolean) = launch { repository.setVideoDoubleTapPlayPause(value) }
    fun setVideoDoubleTapRewind(value: Boolean) = launch { repository.setVideoDoubleTapRewind(value) }
    fun setVideoDoubleTapForward(value: Boolean) = launch { repository.setVideoDoubleTapForward(value) }
    fun setVideoHorizontalSwipeSeek(value: Boolean) = launch { repository.setVideoHorizontalSwipeSeek(value) }
    fun setVideoFullscreenBrightnessSwipe(value: Boolean) = launch { repository.setVideoFullscreenBrightnessSwipe(value) }
    fun setVideoFullscreenVolumeSwipe(value: Boolean) = launch { repository.setVideoFullscreenVolumeSwipe(value) }
    fun setVideoRewindSeconds(value: Int) = launch { repository.setVideoRewindSeconds(value) }
    fun setVideoForwardSeconds(value: Int) = launch { repository.setVideoForwardSeconds(value) }

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
