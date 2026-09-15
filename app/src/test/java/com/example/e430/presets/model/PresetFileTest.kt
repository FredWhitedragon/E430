package com.example.e430.presets.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PresetFileTest {
    @Test
    fun presetFileRoundTripsQueriesAndHomeSelection() {
        val source = PresetFile(
            owner = "ExampleUsername",
            selectedHomePresetId = "wolves",
            presets = listOf(
                SearchPreset("wolves", "Wolves", "wolf order:score"),
                SearchPreset("safe", "Safe", "rating:s"),
            ),
        )

        val encoded = Json.encodeToString(PresetFile.serializer(), source)
        val decoded = Json.decodeFromString(PresetFile.serializer(), encoded)

        assertEquals(source, decoded)
    }
}
