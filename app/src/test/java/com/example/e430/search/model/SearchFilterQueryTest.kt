package com.example.e430.search.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterQueryTest {
    @Test
    fun parseReflectsManuallyEnteredFilters() {
        val filters = SearchFilterQuery.parse(
            "wolf -rating:q date:2025-01-01..2025-02-01 order:comment_count_asc",
        )

        assertEquals(RatingFilter.Questionable, filters.rating)
        assertTrue(filters.ratingExcluded)
        assertEquals(SearchSort.Comments, filters.sort)
        assertTrue(filters.ascending)
        assertEquals("2025-01-01", filters.dateRange.from)
        assertEquals("2025-02-01", filters.dateRange.to)
    }

    @Test
    fun changingExclusiveFiltersRemovesAllPreviousValuesAndAppendsTheNewOne() {
        val ratingQuery = SearchFilterQuery.withRating(
            "rating:s fox -rating:e",
            RatingFilter.Questionable,
            excluded = false,
        )
        val sortedQuery = SearchFilterQuery.withSort(
            "order:score wolf order:id_asc",
            SearchSort.Favorites,
            ascending = false,
        )

        assertEquals("fox rating:q", ratingQuery)
        assertEquals("wolf order:favcount", sortedQuery)
    }

    @Test
    fun directionAndRelativeDateRoundTrip() {
        val ascending = SearchFilterQuery.withSort(
            "fox order:score",
            SearchSort.Score,
            ascending = true,
        )
        val dated = SearchFilterQuery.withDateRange(
            ascending,
            SearchDateRange(from = "1_month_ago", to = ""),
        )
        val parsed = SearchFilterQuery.parse(dated)

        assertEquals("fox order:score_asc date:1_month_ago..", dated)
        assertEquals(SearchSort.Score, parsed.sort)
        assertTrue(parsed.ascending)
        assertEquals("1_month_ago", parsed.dateRange.from)
        assertEquals("", parsed.dateRange.to)
        assertFalse(parsed.ratingExcluded)
    }

    @Test
    fun tappingSelectedOptionsCanClearTheirMetatags() {
        assertEquals("fox", SearchFilterQuery.withRating("fox rating:e", null, false))
        assertEquals("fox", SearchFilterQuery.withSort("order:id fox", null, false))
        assertEquals(
            "fox",
            SearchFilterQuery.withDateRange(
                "fox date:2025-01-01..2025-02-01",
                SearchDateRange(),
            ),
        )
    }

    @Test
    fun dateRangeIsNotAppliedWithoutAStartDate() {
        assertEquals(
            "fox",
            SearchFilterQuery.withDateRange(
                "fox date:2025-01-01..2025-02-01",
                SearchDateRange(from = "", to = "2026-09-15"),
            ),
        )
    }
}
