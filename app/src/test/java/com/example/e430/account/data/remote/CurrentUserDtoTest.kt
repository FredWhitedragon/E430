package com.example.e430.account.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentUserDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_handlesNullableAvatarAndUnknownFields() {
        val user = json.decodeFromString<CurrentUserDto>(
            """{
                "id": 430,
                "name": "example_user",
                "level_string": "Member",
                "favorite_count": 21,
                "post_upload_count": 3,
                "created_at": "2024-01-02T03:04:05Z",
                "avatar_id": null,
                "future_field": true
            }""",
        )

        assertEquals(430L, user.id)
        assertEquals("example_user", user.name)
        assertEquals(21, user.favoriteCount)
        assertNull(user.avatarId)
    }
}
