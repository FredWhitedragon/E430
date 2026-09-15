package com.example.e430.presets.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchPreset(
    val id: String,
    val name: String,
    val query: String = "",
)

@Serializable
data class PresetFile(
    val owner: String = "",
    val selectedHomePresetId: String? = null,
    val presets: List<SearchPreset> = emptyList(),
)
