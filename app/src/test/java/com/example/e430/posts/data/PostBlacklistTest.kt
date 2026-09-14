package com.example.e430.posts.data

import com.example.e430.posts.data.remote.PostThumbnailDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostBlacklistTest {
    @Test
    fun rulesUseOrBetweenLinesAndAndWithinLine() {
        val blacklist = PostBlacklist("wolf gore\nfox rating:e")

        assertTrue(blacklist.blocks(post(tags = "wolf gore", rating = "s")))
        assertTrue(blacklist.blocks(post(tags = "fox solo", rating = "e")))
        assertFalse(blacklist.blocks(post(tags = "wolf solo", rating = "s")))
    }

    @Test
    fun negativeWildcardAndNumericConditionsAreSupported() {
        val blacklist = PostBlacklist("canine* -domestic_dog score:<0")

        assertTrue(blacklist.blocks(post(tags = "canine anthro", score = -2)))
        assertFalse(blacklist.blocks(post(tags = "canine domestic_dog", score = -2)))
        assertFalse(blacklist.blocks(post(tags = "canine anthro", score = 4)))
    }

    private fun post(
        tags: String,
        rating: String = "s",
        score: Int = 0,
    ) = PostThumbnailDto(
        id = 42,
        tags = tags,
        rating = rating,
        score = score,
    )
}
