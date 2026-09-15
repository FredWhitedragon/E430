package com.example.e430.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.e430.R
import com.example.e430.posts.model.HomeSort
import com.example.e430.presets.model.SearchPreset
import com.example.e430.settings.model.ImageQualityPreference

@Composable
fun SettingsScreen(
    homeSort: HomeSort,
    presets: List<SearchPreset>,
    selectedHomePresetId: String?,
    meteredImageQuality: ImageQualityPreference,
    wifiImageQuality: ImageQualityPreference,
    videoAutoPlay: Boolean,
    videoMuted: Boolean,
    tagsCollapsed: Boolean,
    downloadDirectory: String,
    prefetchOnMetered: Boolean,
    videoLoop: Boolean,
    onHomeSortChange: (HomeSort) -> Unit,
    onPresetSelected: (SearchPreset) -> Unit,
    onCreatePreset: () -> Unit,
    onMeteredImageQualityChange: (ImageQualityPreference) -> Unit,
    onWifiImageQualityChange: (ImageQualityPreference) -> Unit,
    onVideoAutoPlayChange: (Boolean) -> Unit,
    onVideoMutedChange: (Boolean) -> Unit,
    onTagsCollapsedChange: (Boolean) -> Unit,
    onDownloadDirectoryChange: (String) -> Unit,
    onPrefetchOnMeteredChange: (Boolean) -> Unit,
    onVideoLoopChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.home_feed_order),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.home_feed_order_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        HomeSort.entries.forEach { sort ->
            val label = when (sort) {
                HomeSort.Latest -> R.string.latest_first
                HomeSort.Popular -> R.string.highest_score_first
                HomeSort.Custom -> R.string.custom
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHomeSortChange(sort) }
                    .padding(vertical = 8.dp),
            ) {
                RadioButton(
                    selected = homeSort == sort,
                    onClick = { onHomeSortChange(sort) },
                )
                Text(
                    text = stringResource(label),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (sort == HomeSort.Custom && homeSort == HomeSort.Custom) {
                if (presets.isEmpty()) {
                    OutlinedButton(
                        onClick = onCreatePreset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, bottom = 8.dp),
                    ) {
                        Text(stringResource(R.string.create_preset))
                    }
                } else {
                    PresetSelector(
                        presets = presets,
                        selectedId = selectedHomePresetId,
                        onSelected = onPresetSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, bottom = 8.dp),
                    )
                }
            }
        }
        SettingsSectionDivider()
        Text(
            text = stringResource(R.string.media_defaults),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.image_quality_metered),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
        ImageQualitySelector(meteredImageQuality, onMeteredImageQualityChange)
        Text(
            text = stringResource(R.string.image_quality_wifi),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
        ImageQualitySelector(wifiImageQuality, onWifiImageQualityChange)
        SettingsSwitchRow(
            title = stringResource(R.string.prefetch_on_metered),
            checked = prefetchOnMetered,
            onCheckedChange = onPrefetchOnMeteredChange,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.video_auto_play),
            checked = videoAutoPlay,
            onCheckedChange = onVideoAutoPlayChange,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.video_muted),
            checked = videoMuted,
            onCheckedChange = onVideoMutedChange,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.video_loop),
            checked = videoLoop,
            onCheckedChange = onVideoLoopChange,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.tags_collapsed_default),
            checked = tagsCollapsed,
            onCheckedChange = onTagsCollapsedChange,
        )
        OutlinedTextField(
            value = downloadDirectory,
            onValueChange = onDownloadDirectoryChange,
            label = { Text(stringResource(R.string.download_directory)) },
            supportingText = { Text(stringResource(R.string.download_directory_description)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun ImageQualitySelector(
    selected: ImageQualityPreference,
    onSelected: (ImageQualityPreference) -> Unit,
) {
    ImageQualityPreference.entries.forEach { quality ->
        val label = when (quality) {
            ImageQualityPreference.Low -> R.string.quality_low
            ImageQualityPreference.Medium -> R.string.quality_medium
            ImageQualityPreference.Original -> R.string.quality_original
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected(quality) },
        ) {
            RadioButton(
                selected = selected == quality,
                onClick = { onSelected(quality) },
            )
            Text(stringResource(label), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PresetSelector(
    presets: List<SearchPreset>,
    selectedId: String?,
    onSelected: (SearchPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = presets.firstOrNull { it.id == selectedId }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.name ?: stringResource(R.string.choose_preset))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.sortedBy { it.name.lowercase() }.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.name) },
                    onClick = {
                        expanded = false
                        onSelected(preset)
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(20.dp))
}
