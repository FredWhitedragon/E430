package com.example.e430.posts.ui

import java.util.Locale
import kotlin.math.abs

internal fun compactPostStat(value: Int): String {
    val negative = value < 0
    var scaled = abs(value.toLong()).toDouble()
    val suffixes = arrayOf("", "k", "m", "b")
    var suffixIndex = 0
    while (scaled >= 1_000.0 && suffixIndex < suffixes.lastIndex) {
        scaled /= 1_000.0
        suffixIndex += 1
    }
    if (suffixIndex == 0) return value.toString()
    if (scaled >= 999.5 && suffixIndex < suffixes.lastIndex) {
        scaled /= 1_000.0
        suffixIndex += 1
    }
    val number = if (scaled < 10.0) {
        String.format(Locale.ROOT, "%.1f", scaled).removeSuffix(".0")
    } else {
        String.format(Locale.ROOT, "%.0f", scaled)
    }
    return buildString {
        if (negative) append('-')
        append(number)
        append(suffixes[suffixIndex])
    }
}
