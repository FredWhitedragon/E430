package com.example.e430.posts.detail.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PostDetailTimeFormatterTest {
    @Test
    fun formatPlaybackTime_formatsMinutesAndSeconds() {
        assertEquals("00:00/00:00", formatPlaybackTime(0L, 0L))
        assertEquals("01:05/12:34", formatPlaybackTime(65_999L, 754_000L))
        assertEquals("61:01/120:00", formatPlaybackTime(3_661_000L, 7_200_000L))
    }

    @Test
    fun formatDurationSeconds_roundsFractionalSeconds() {
        assertEquals("01:05", formatDurationSeconds(65.49f))
        assertEquals("01:05", formatDurationSeconds(64.5f))
        assertEquals("01:06", formatDurationSeconds(65.5f))
        assertEquals("60:00", formatDurationSeconds(3_599.6f))
    }
}
