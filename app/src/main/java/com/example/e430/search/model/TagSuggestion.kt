package com.example.e430.search.model

data class TagSuggestion(
    val name: String,
    val category: Int,
    val postCount: Int? = null,
)

data class SearchCompletionTarget(
    val start: Int,
    val end: Int,
    val queryPrefix: String,
    val operator: String,
) {
    val usesLocalMetaTags: Boolean = ':' in queryPrefix
}

/** Finds the field containing the cursor while using only text to its left for matching. */
fun searchCompletionTarget(text: String, cursor: Int): SearchCompletionTarget? {
    val safeCursor = cursor.coerceIn(0, text.length)
    if (safeCursor == 0 || text[safeCursor - 1].isWhitespace()) return null

    val start = text.lastIndexOfAny(charArrayOf(' ', '\t', '\n'), safeCursor - 1) + 1
    var end = safeCursor
    while (end < text.length && !text[end].isWhitespace()) end += 1

    val leftOfCursor = text.substring(start, safeCursor)
    val operator = leftOfCursor.takeWhile { it == '-' || it == '~' }
    val queryPrefix = leftOfCursor.removePrefix(operator)
    if (queryPrefix.isBlank()) return null

    return SearchCompletionTarget(
        start = start,
        end = end,
        queryPrefix = queryPrefix,
        operator = operator,
    )
}

fun replaceSearchCompletion(
    text: String,
    target: SearchCompletionTarget,
    suggestion: String,
): Pair<String, Int> {
    var replaceEnd = target.end
    while (replaceEnd < text.length && text[replaceEnd].isWhitespace()) replaceEnd += 1
    val replacement = target.operator + suggestion + " "
    val updated = text.replaceRange(target.start, replaceEnd, replacement)
    return updated to (target.start + replacement.length)
}
