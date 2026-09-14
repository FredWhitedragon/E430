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
    val customHomeQuery: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[CUSTOM_HOME_QUERY].orEmpty()
    }
    val imageQuality: Flow<ImageQualityPreference> = context.settingsDataStore.data.map { preferences ->
        ImageQualityPreference.entries.firstOrNull { it.name == preferences[IMAGE_QUALITY] }
            ?: ImageQualityPreference.Auto
    }
    val videoAutoPlay: Flow<Boolean> = context.settingsDataStore.data.map { it[VIDEO_AUTO_PLAY] ?: false }
    val videoMuted: Flow<Boolean> = context.settingsDataStore.data.map { it[VIDEO_MUTED] ?: true }
    val tagsCollapsed: Flow<Boolean> = context.settingsDataStore.data.map { it[TAGS_COLLAPSED] ?: true }
    val downloadDirectory: Flow<String> = context.settingsDataStore.data.map {
        it[DOWNLOAD_DIRECTORY]?.takeIf(String::isNotBlank) ?: "E430"
    }

    suspend fun setHomeSort(sort: HomeSort) {
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_SORT] = sort.name
        }
    }

    suspend fun setCustomHomeQuery(query: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[CUSTOM_HOME_QUERY] = query
        }
    }

    suspend fun setImageQuality(value: ImageQualityPreference) = edit(IMAGE_QUALITY, value.name)
    suspend fun setVideoAutoPlay(value: Boolean) = edit(VIDEO_AUTO_PLAY, value)
    suspend fun setVideoMuted(value: Boolean) = edit(VIDEO_MUTED, value)
    suspend fun setTagsCollapsed(value: Boolean) = edit(TAGS_COLLAPSED, value)
    suspend fun setDownloadDirectory(value: String) = edit(DOWNLOAD_DIRECTORY, value)

    private suspend fun <T> edit(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private companion object {
        val HOME_SORT = stringPreferencesKey("home_sort")
        val CUSTOM_HOME_QUERY = stringPreferencesKey("custom_home_query")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val VIDEO_AUTO_PLAY = booleanPreferencesKey("video_auto_play")
        val VIDEO_MUTED = booleanPreferencesKey("video_muted")
        val TAGS_COLLAPSED = booleanPreferencesKey("tags_collapsed")
        val DOWNLOAD_DIRECTORY = stringPreferencesKey("download_directory")
    }
}
