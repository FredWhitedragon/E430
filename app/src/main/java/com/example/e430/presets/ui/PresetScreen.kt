package com.example.e430.presets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.e430.R
import com.example.e430.presets.model.SearchPreset

@Composable
fun PresetScreen(
    state: PresetUiState,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onQueryChange: (String, String) -> Unit,
    onChooseStorage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var createDialogVisible by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SearchPreset?>(null) }
    when {
        state.isLoading -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize(),
        ) { CircularProgressIndicator() }
        state.storageAccessRequired -> StorageAccessState(onChooseStorage, modifier)
        state.loadFailed -> ErrorState(onRetry, modifier)
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Button(onClick = { createDialogVisible = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.new_preset))
            }
            if (state.saveFailed) {
                Text(
                    stringResource(R.string.preset_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (state.presets.isEmpty()) {
                Text(
                    stringResource(R.string.no_presets),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            state.presets.forEach { preset ->
                Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            preset.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { renaming = preset }) {
                            Icon(
                                painterResource(R.drawable.ic_edit),
                                stringResource(R.string.rename_preset),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = preset.query,
                        onValueChange = { onQueryChange(preset.id, it) },
                        label = { Text(stringResource(R.string.preset_search_query)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (createDialogVisible) {
        PresetNameDialog(
            title = stringResource(R.string.new_preset),
            initialValue = "",
            onDismiss = { createDialogVisible = false },
            onConfirm = { onCreate(it); createDialogVisible = false },
        )
    }
    renaming?.let { preset ->
        PresetNameDialog(
            title = stringResource(R.string.rename_preset),
            initialValue = preset.name,
            onDismiss = { renaming = null },
            onConfirm = { onRename(preset.id, it); renaming = null },
        )
    }
}

@Composable
private fun PresetNameDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.preset_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun StorageAccessState(onChooseStorage: () -> Unit, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        Text(stringResource(R.string.preset_storage_access), color = MaterialTheme.colorScheme.onSurface)
        Button(onClick = onChooseStorage, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.choose_documents_folder))
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(24.dp),
    ) {
        Text(stringResource(R.string.preset_load_failed), color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}
