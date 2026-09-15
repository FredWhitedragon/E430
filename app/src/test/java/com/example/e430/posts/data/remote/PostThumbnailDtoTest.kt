package com.example.e430.posts.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostThumbnailDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_handlesNullablePreviewAndUnknownFields() {
        val dto = json.decodeFromString<PostThumbnailDto>(
            """{
                "id": 42,
                "preview_url": null,
                "preview_webp": null,
                "score": 12,
                "fav_count": 3,
                "comment_count": 4,
                "rating": "q",
                "file_ext": "webm",
                "tags": "wolf animated",
                "future_field": true
            }""",
        )

        assertEquals(42L, dto.id)
        assertNull(dto.previewUrl)
        assertEquals(12, dto.score)
        assertEquals("q", dto.rating)
        assertEquals("webm", dto.fileExtension)
        assertEquals("wolf animated", dto.tags)
    }
}
