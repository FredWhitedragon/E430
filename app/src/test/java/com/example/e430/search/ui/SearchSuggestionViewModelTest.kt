package com.example.e430.search.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionViewModelTest {
    @Test
    fun localMetatagSuggestionsAreNotLimitedToFiveItems() {
        val suggestions = SearchSuggestionViewModel.localMetaTagSuggestions("order:")

        assertTrue(suggestions.size > 5)
        assertEquals("order:id", suggestions.first().name)
        assertEquals("order:comment_count_asc", suggestions.last().name)
    }
}
