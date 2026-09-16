package com.example.e430.search.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCompletionTest {
    @Test
    fun usesOnlyCharactersLeftOfCursorForMatching() {
        val target = searchCompletionTarget("fox dragon", cursor = 7)

        assertEquals(4, target?.start)
        assertEquals(10, target?.end)
        assertEquals("dra", target?.queryPrefix)
        assertFalse(requireNotNull(target).usesLocalMetaTags)
    }

    @Test
    fun startsLocalMatchingOnlyAfterColonIsLeftOfCursor() {
        val beforeColon = searchCompletionTarget("rating:e", cursor = 6)
        val afterColon = searchCompletionTarget("rating:e", cursor = 7)

        assertFalse(requireNotNull(beforeColon).usesLocalMetaTags)
        assertTrue(requireNotNull(afterColon).usesLocalMetaTags)
    }

    @Test
    fun noTargetExistsWhenCursorHasNoCharacterOnItsLeft() {
        assertNull(searchCompletionTarget("fox", cursor = 0))
        assertNull(searchCompletionTarget("fox ", cursor = 4))
    }

    @Test
    fun replacementPreservesOperatorAndLeavesOneTrailingSpace() {
        val text = "fox -dragonn   wolf"
        val target = requireNotNull(searchCompletionTarget(text, cursor = 9))

        val (updated, cursor) = replaceSearchCompletion(text, target, "dragon")

        assertEquals("fox -dragon wolf", updated)
        assertEquals(12, cursor)
    }
}
