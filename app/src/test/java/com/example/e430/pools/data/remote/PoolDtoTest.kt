package com.example.e430.pools.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PoolDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_preservesDescriptionAndPostOrder() {
        val dto = json.decodeFromString<PoolDto>(
            """{
                "id": 51,
                "name": "example_pool",
                "description": "Ordered example",
                "post_ids": [30, 10, 20],
                "post_count": 3,
                "future_field": true
            }""",
        )

        assertEquals(51L, dto.id)
        assertEquals("Ordered example", dto.description)
        assertEquals(listOf(30L, 10L, 20L), dto.postIds)
        assertEquals(3, dto.postCount)
    }
}
