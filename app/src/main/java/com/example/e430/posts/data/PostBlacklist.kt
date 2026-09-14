package com.example.e430.posts.data

import com.example.e430.posts.data.remote.PostThumbnailDto

/** Matches the per-line tag expressions stored in an e621/e926 user blacklist. */
internal class PostBlacklist(source: String) {
    private val rules = source
        .lineSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line -> line.split(Regex("\\s+")).filter(String::isNotBlank) }
        .toList()

    fun blocks(post: PostThumbnailDto): Boolean {
        if (rules.isEmpty()) return false
        val postTags = post.tags
            .splitToSequence(' ')
            .filter(String::isNotBlank)
            .map(String::lowercase)
            .toSet()
        return rules.any { tokens ->
            val alternatives = tokens.filter { it.startsWith("~") }
            val required = tokens.filterNot { it.startsWith("~") }
            required.all { tokenMatches(it, post, postTags) } &&
                (alternatives.isEmpty() || alternatives.any {
                    tokenMatches(it.removePrefix("~"), post, postTags)
                })
        }
    }

    private fun tokenMatches(
        token: String,
        post: PostThumbnailDto,
        postTags: Set<String>,
    ): Boolean {
        val excluded = token.startsWith("-")
        val expression = token.removePrefix("-").lowercase()
        val matches = when {
            expression.startsWith("rating:") -> {
                val wanted = expression.substringAfter(':')
                post.rating.lowercase() in wanted.split(',').map(::ratingCode)
            }
            expression.startsWith("score:") -> compareNumber(post.score.toLong(), expression.substringAfter(':'))
            expression.startsWith("favcount:") -> compareNumber(post.favoriteCount.toLong(), expression.substringAfter(':'))
            expression.startsWith("comment_count:") -> compareNumber(post.commentCount.toLong(), expression.substringAfter(':'))
            expression.startsWith("id:") -> compareNumber(post.id, expression.substringAfter(':'))
            expression.startsWith("user:") || expression.startsWith("uploader:") -> {
                compareNumber(post.uploaderId, expression.substringAfter(':'))
            }
            expression.startsWith("status:") -> expression.substringAfter(':') in post.flags.split(' ')
            '*' in expression -> wildcardRegex(expression).matchesAny(postTags)
            else -> expression in postTags
        }
        return if (excluded) !matches else matches
    }

    private fun compareNumber(actual: Long, expression: String): Boolean {
        val match = NUMBER_EXPRESSION.matchEntire(expression) ?: return false
        val expected = match.groupValues[2].toLongOrNull() ?: return false
        return when (match.groupValues[1]) {
            ">" -> actual > expected
            ">=" -> actual >= expected
            "<" -> actual < expected
            "<=" -> actual <= expected
            else -> actual == expected
        }
    }

    private fun ratingCode(value: String): String = when (value.trim()) {
        "safe" -> "s"
        "questionable" -> "q"
        "explicit" -> "e"
        else -> value.trim()
    }

    private fun wildcardRegex(expression: String): Regex = Regex(
        expression
            .split('*')
            .joinToString(".*") { Regex.escape(it) },
        RegexOption.IGNORE_CASE,
    )

    private fun Regex.matchesAny(tags: Set<String>): Boolean = tags.any(::matches)

    private companion object {
        val NUMBER_EXPRESSION = Regex("(>=|<=|>|<|=)?(-?\\d+)")
    }
}
