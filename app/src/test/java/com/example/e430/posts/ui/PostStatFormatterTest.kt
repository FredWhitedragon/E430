package com.example.e430.posts.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PostStatFormatterTest {
    @Test
    fun compactPostStat_usesReadableSuffixes() {
        assertEquals("999", compactPostStat(999))
        assertEquals("1k", compactPostStat(1_000))
        assertEquals("1.5k", compactPostStat(1_500))
        assertEquals("12k", compactPostStat(12_345))
        assertEquals("1m", compactPostStat(999_999))
        assertEquals("-2.5k", compactPostStat(-2_500))
    }
}
