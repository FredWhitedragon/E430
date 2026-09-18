package com.example.e430.posts.detail.data.remote

import com.example.e430.posts.detail.data.toModel
import com.example.e430.posts.detail.model.MediaKind
import com.example.e430.posts.detail.model.MediaQuality
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

    @Test
    fun videoMapping_usesPlayableAlternatesAndNeverPreviewImage() {
        val response = json.decodeFromString<PostDetailResponseDto>(
            """{"post":{"id":431,"file":{"width":1920,"height":1080,"ext":"webm","size":9000000,"url":"https://example.test/original.webm"},"preview":{"width":150,"height":84,"url":"https://example.test/preview.jpg"},"sample":{"width":1280,"height":720,"url":"https://example.test/sample.jpg","alternates":{"samples":{"480p":{"size":1000000,"width":854,"height":480,"url":"https://example.test/480.mp4"},"720p":{"size":2500000,"width":1280,"height":720,"url":"https://example.test/720.mp4"}}}},"score":{"up":10,"down":2,"total":8},"tags":{"general":["video"]},"rating":"s","fav_count":4}}""",
        )

        val post = response.post.toModel()

        assertEquals(MediaKind.Video, post.kind)
        assertEquals(
            listOf(MediaQuality.Low, MediaQuality.Medium, MediaQuality.Original),
            post.mediaSources.map { it.quality },
        )
        assertEquals(listOf(480, 720, 1080), post.mediaSources.map { it.height })
        assertEquals(false, post.mediaSources.any { it.url.endsWith(".jpg") })
    }
}
