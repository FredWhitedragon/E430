package com.example.e430.search.model

enum class RatingFilter(val queryValue: String) {
    Safe("s"),
    Questionable("q"),
    Explicit("e"),
}

enum class SearchSort(val queryValue: String) {
    Date("id"),
    Favorites("favcount"),
    Score("score"),
    Comments("comment_count"),
}

data class SearchDateRange(
    val from: String = "",
    val to: String = "",
)

data class SearchFilters(
    val rating: RatingFilter? = null,
    val ratingExcluded: Boolean = false,
    val sort: SearchSort? = null,
    val ascending: Boolean = false,
    val dateRange: SearchDateRange = SearchDateRange(),
)

/** Keeps the filter controls and the editable e621 query as one source of truth. */
object SearchFilterQuery {
    private val ratingPattern = Regex("^(-)?rating:([sqe])$", RegexOption.IGNORE_CASE)
    private val sortPattern = Regex(
        "^order:(id|date|favcount|score|comment_count)(?:_(asc|desc))?$",
        RegexOption.IGNORE_CASE,
    )
    private val datePattern = Regex("^date:(\\S+)$", RegexOption.IGNORE_CASE)

    fun parse(query: String): SearchFilters {
        var rating: RatingFilter? = null
        var ratingExcluded = false
        var sort: SearchSort? = null
        var ascending = false
        var dateRange = SearchDateRange()

        tokens(query).forEach { token ->
            ratingPattern.matchEntire(token)?.let { match ->
                rating = RatingFilter.entries.firstOrNull {
                    it.queryValue == match.groupValues[2].lowercase()
                }
                ratingExcluded = match.groupValues[1] == "-"
            }
            sortPattern.matchEntire(token)?.let { match ->
                sort = when (match.groupValues[1].lowercase()) {
                    "id", "date" -> SearchSort.Date
                    "favcount" -> SearchSort.Favorites
                    "score" -> SearchSort.Score
                    "comment_count" -> SearchSort.Comments
                    else -> null
                }
                ascending = match.groupValues[2].equals("asc", ignoreCase = true)
            }
            datePattern.matchEntire(token)?.let { match ->
                val value = match.groupValues[1]
                dateRange = if (".." in value) {
                    val (from, to) = value.split("..", limit = 2)
                    SearchDateRange(from = from, to = to)
                } else {
                    SearchDateRange(from = value, to = value)
                }
            }
        }
        return SearchFilters(rating, ratingExcluded, sort, ascending, dateRange)
    }

    fun withRating(
        query: String,
        rating: RatingFilter?,
        excluded: Boolean,
    ): String = replace(query, ratingPattern) {
        rating?.let { "${if (excluded) "-" else ""}rating:${it.queryValue}" }
    }

    fun withSort(
        query: String,
        sort: SearchSort?,
        ascending: Boolean,
    ): String = replace(query, sortPattern) {
        sort?.let { "order:${it.queryValue}${if (ascending) "_asc" else ""}" }
    }

    fun withDateRange(query: String, range: SearchDateRange): String =
        replace(query, datePattern) {
            when {
                range.from.isBlank() -> null
                range.from == range.to -> "date:${range.from.trim()}"
                else -> "date:${range.from.trim()}..${range.to.trim()}"
            }
        }

    private fun replace(query: String, pattern: Regex, replacement: () -> String?): String {
        val retained = tokens(query).filterNot(pattern::matches).toMutableList()
        replacement()?.takeIf(String::isNotBlank)?.let(retained::add)
        return retained.joinToString(" ")
    }

    private fun tokens(query: String): List<String> = query.trim().split(Regex("\\s+"))
        .filter(String::isNotBlank)
}
