package com.example.e430.posts.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PostRepositoryTest {
    private val repository = PostRepository(emptyMap())

    @Test
    fun buildTags_replacesUserProvidedOrderWithFeedOrder() {
        val tags = repository.buildTags(
            query = "fox order:favcount rating:s",
            order = "order:id_desc",
        )

        assertEquals("fox rating:s order:id_desc", tags)
    }

    @Test
    fun buildTags_withEmptyQueryOnlyUsesFeedOrder() {
        assertEquals("order:score", repository.buildTags("", "order:score"))
    }

    @Test
    fun buildTags_withoutForcedOrderPreservesCustomSearch() {
        assertEquals(
            "rating:s order:favcount fox",
            repository.buildTags("  rating:s order:favcount fox  ", null),
        )
    }
}
