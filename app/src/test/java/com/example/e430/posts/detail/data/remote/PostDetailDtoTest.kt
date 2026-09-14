package com.example.e430.posts.detail.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostDetailDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_handlesLegacyDetailWithUnavailableMediaVariants() {
        val response = json.decodeFromString<PostDetailResponseDto>(
            """{"post":{"id":430,"file":{"width":1000,"height":800,"ext":"jpg","size":12345,"url":"https://example.test/original.jpg"},"preview":{"width":150,"height":120,"url":null},"sample":{"width":850,"height":680,"url":"https://example.test/sample.jpg","alternates":{}},"score":{"up":10,"down":2,"total":8},"tags":{"general":["test_tag"]},"rating":"s","fav_count":4,"is_favorited":false,"vote":0}}""",
        )

        assertEquals(430L, response.post.id)
        assertNull(response.post.preview.url)
        assertEquals(listOf("test_tag"), response.post.tags.general)
        assertEquals(8, response.post.score.total)
    }
}
