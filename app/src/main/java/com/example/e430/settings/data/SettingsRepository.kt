package com.example.e430.settings.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.e430.posts.model.HomeSort
import com.example.e430.settings.model.ImageQualityPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    val homeSort: Flow<HomeSort> = context.settingsDataStore.data.map { preferences ->
        when (preferences[HOME_SORT]) {
            HomeSort.Popular.name -> HomeSort.Popular
            HomeSort.Custom.name -> HomeSort.Custom
            else -> HomeSort.Latest
        }
    }
    val meteredImageQuality: Flow<ImageQualityPreference> = context.settingsDataStore.data.map { preferences ->
        ImageQualityPreference.entries.firstOrNull { it.name == preferences[METERED_IMAGE_QUALITY] }
            ?: ImageQualityPreference.Low
    }
    val wifiImageQuality: Flow<ImageQualityPreference> = context.settingsDataStore.data.map { preferences ->
        ImageQualityPreference.entries.firstOrNull { it.name == preferences[WIFI_IMAGE_QUALITY] }
            ?: ImageQualityPreference.Medium
    }
    val videoAutoPlay: Flow<Boolean> = context.settingsDataStore.data.map { it[VIDEO_AUTO_PLAY] ?: false }
    val videoMuted: Flow<Boolean> = context.settingsDataStore.data.map { it[VIDEO_MUTED] ?: true }
    val tagsCollapsed: Flow<Boolean> = context.settingsDataStore.data.map { it[TAGS_COLLAPSED] ?: true }
    val downloadDirectory: Flow<String> = context.settingsDataStore.data.map {
        it[DOWNLOAD_DIRECTORY]?.takeIf(String::isNotBlank) ?: "E430"
    }
    val prefetchOnMetered: Flow<Boolean> = context.settingsDataStore.data.map { it[PREFETCH_ON_METERED] ?: false }
    val videoLoop: Flow<Boolean> = context.settingsDataStore.data.map { it[VIDEO_LOOP] ?: true }

    suspend fun setHomeSort(sort: HomeSort) {
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_SORT] = sort.name
        }
    }

    suspend fun setMeteredImageQuality(value: ImageQualityPreference) = edit(METERED_IMAGE_QUALITY, value.name)
    suspend fun setWifiImageQuality(value: ImageQualityPreference) = edit(WIFI_IMAGE_QUALITY, value.name)
    suspend fun setVideoAutoPlay(value: Boolean) = edit(VIDEO_AUTO_PLAY, value)
    suspend fun setVideoMuted(value: Boolean) = edit(VIDEO_MUTED, value)
    suspend fun setTagsCollapsed(value: Boolean) = edit(TAGS_COLLAPSED, value)
    suspend fun setDownloadDirectory(value: String) = edit(DOWNLOAD_DIRECTORY, value)
    suspend fun setPrefetchOnMetered(value: Boolean) = edit(PREFETCH_ON_METERED, value)
    suspend fun setVideoLoop(value: Boolean) = edit(VIDEO_LOOP, value)

    private suspend fun <T> edit(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private companion object {
        val HOME_SORT = stringPreferencesKey("home_sort")
        val METERED_IMAGE_QUALITY = stringPreferencesKey("metered_image_quality")
        val WIFI_IMAGE_QUALITY = stringPreferencesKey("wifi_image_quality")
        val VIDEO_AUTO_PLAY = booleanPreferencesKey("video_auto_play")
        val VIDEO_MUTED = booleanPreferencesKey("video_muted")
        val TAGS_COLLAPSED = booleanPreferencesKey("tags_collapsed")
        val DOWNLOAD_DIRECTORY = stringPreferencesKey("download_directory")
        val PREFETCH_ON_METERED = booleanPreferencesKey("prefetch_on_metered")
        val VIDEO_LOOP = booleanPreferencesKey("video_loop")
    }
}
