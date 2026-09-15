package com.example.e430.presets.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.presets.data.PresetRepository
import com.example.e430.presets.model.PresetFile
import com.example.e430.presets.model.SearchPreset
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PresetUiState(
    val owner: String = "anonymous",
    val presets: List<SearchPreset> = emptyList(),
    val selectedHomePresetId: String? = null,
    val isLoading: Boolean = true,
    val storageAccessRequired: Boolean = false,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
)

class PresetViewModel(private val repository: PresetRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PresetUiState())
    val uiState: StateFlow<PresetUiState> = _uiState.asStateFlow()
    private var ownerJob: Job? = null
    private var saveJob: Job? = null

    fun setOwner(owner: String) {
        val normalizedOwner = owner.ifBlank { "anonymous" }
        if (_uiState.value.owner == normalizedOwner && !_uiState.value.isLoading) return
        ownerJob?.cancel()
        saveJob?.cancel()
        _uiState.value = PresetUiState(owner = normalizedOwner)
        if (!repository.hasStorageAccess()) {
            _uiState.update { it.copy(isLoading = false, storageAccessRequired = true) }
            return
        }
        ownerJob = viewModelScope.launch { load(normalizedOwner) }
    }

    fun grantStorageAccess(uri: Uri?) {
        if (uri == null || !repository.takeStorageAccess(uri)) {
            _uiState.update { it.copy(isLoading = false, storageAccessRequired = true) }
            return
        }
        val owner = _uiState.value.owner
        _uiState.update { it.copy(isLoading = true, storageAccessRequired = false, loadFailed = false) }
        ownerJob?.cancel()
        ownerJob = viewModelScope.launch { load(owner) }
    }

    fun retry() {
        val owner = _uiState.value.owner
        _uiState.value = PresetUiState(owner = owner)
        setOwner(owner)
    }

    fun create(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        val preset = SearchPreset(id = UUID.randomUUID().toString(), name = trimmedName)
        _uiState.update { it.copy(presets = it.presets + preset, saveFailed = false) }
        saveNow()
    }

    fun rename(id: String, name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                presets = state.presets.map { if (it.id == id) it.copy(name = trimmedName) else it },
                saveFailed = false,
            )
        }
        saveNow()
    }

    fun updateQuery(id: String, query: String) {
        _uiState.update { state ->
            state.copy(
                presets = state.presets.map { if (it.id == id) it.copy(query = query) else it },
                saveFailed = false,
            )
        }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            saveCurrent()
        }
    }

    fun selectHomePreset(id: String) {
        _uiState.update { it.copy(selectedHomePresetId = id, saveFailed = false) }
        saveNow()
    }

    private suspend fun load(owner: String) {
        try {
            val file = repository.load(owner)
            if (_uiState.value.owner == owner) {
                _uiState.value = PresetUiState(
                    owner = owner,
                    presets = file.presets,
                    selectedHomePresetId = file.selectedHomePresetId
                        ?.takeIf { selected -> file.presets.any { it.id == selected } },
                    isLoading = false,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            if (_uiState.value.owner == owner) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private fun saveNow() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { saveCurrent() }
    }

    private suspend fun saveCurrent() {
        val snapshot = _uiState.value
        try {
            repository.save(
                snapshot.owner,
                PresetFile(
                    owner = snapshot.owner,
                    selectedHomePresetId = snapshot.selectedHomePresetId,
                    presets = snapshot.presets,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            if (_uiState.value.owner == snapshot.owner) {
                _uiState.update { it.copy(saveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(repository: PresetRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PresetViewModel(repository) as T
            }
    }
}
